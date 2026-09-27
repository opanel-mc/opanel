use std::{
    net::{IpAddr, Ipv4Addr},
    sync::{Arc, PoisonError},
};

use axum::{
    extract::{Query, State},
    http::StatusCode,
    response::{IntoResponse, Response},
};
use pumpkin::data::{
    SaveJSONConfiguration, banlist_serializer::BannedIpEntry, banned_ip::BannedIpList,
};
use serde::{Deserialize, Serialize};
use time::OffsetDateTime;

use crate::{
    opanel::OPanel,
    web::{
        controller::control::EmptyPayload,
        response::{ApiError, ApiResponse},
    },
};

const DEFAULT_BAN_SOURCE: &str = "(Unknown)";
const DEFAULT_BAN_REASON: &str = "Banned by an operator.";

#[derive(Debug, Deserialize)]
pub(super) struct IpQuery {
    ip: Option<String>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct BannedIpsPayload {
    banned_ips: Vec<String>,
}

pub(super) async fn get_banned_ips(State(opanel): State<Arc<OPanel>>) -> Response {
    let server = &opanel.context().server;
    let mut banned_ips = server
        .data
        .banned_ip_list
        .write()
        .unwrap_or_else(PoisonError::into_inner);
    if prune_expired(&mut banned_ips, OffsetDateTime::now_utc()) {
        banned_ips.save();
    }

    ApiResponse::ok(BannedIpsPayload {
        banned_ips: banned_ips
            .banned_ips
            .iter()
            .map(|entry| entry.ip.to_string())
            .collect(),
    })
    .into_response()
}

pub(super) async fn ban_ip(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<IpQuery>,
) -> Response {
    let ip = match parse_ipv4(query.ip.as_deref()) {
        Ok(ip) => ip,
        Err(error) => return error.into_response(),
    };

    let server = &opanel.context().server;
    let mut banned_ips = server
        .data
        .banned_ip_list
        .write()
        .unwrap_or_else(PoisonError::into_inner);
    let mut changed = prune_expired(&mut banned_ips, OffsetDateTime::now_utc());
    changed |= add_ban(&mut banned_ips, ip);
    if changed {
        banned_ips.save();
    }

    ApiResponse::ok(EmptyPayload {}).into_response()
}

pub(super) async fn pardon_ip(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<IpQuery>,
) -> Response {
    let ip = match parse_ipv4(query.ip.as_deref()) {
        Ok(ip) => ip,
        Err(error) => return error.into_response(),
    };

    let server = &opanel.context().server;
    let mut banned_ips = server
        .data
        .banned_ip_list
        .write()
        .unwrap_or_else(PoisonError::into_inner);
    let mut changed = prune_expired(&mut banned_ips, OffsetDateTime::now_utc());
    changed |= remove_ban(&mut banned_ips, ip);
    if changed {
        banned_ips.save();
    }

    ApiResponse::ok(EmptyPayload {}).into_response()
}

fn parse_ipv4(ip: Option<&str>) -> Result<IpAddr, ApiError> {
    let Some(ip) = ip else {
        return Err(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Ip is missing or the ip address is illegal.",
        ));
    };
    ip.parse::<Ipv4Addr>().map(IpAddr::V4).map_err(|_| {
        ApiError::new(
            StatusCode::BAD_REQUEST,
            "Ip is missing or the ip address is illegal.",
        )
    })
}

fn add_ban(banned_ips: &mut BannedIpList, ip: IpAddr) -> bool {
    if banned_ips.banned_ips.iter().any(|entry| entry.ip == ip) {
        return false;
    }

    banned_ips.banned_ips.push(BannedIpEntry::new(
        ip,
        DEFAULT_BAN_SOURCE.to_owned(),
        None,
        DEFAULT_BAN_REASON.to_owned(),
    ));
    true
}

fn remove_ban(banned_ips: &mut BannedIpList, ip: IpAddr) -> bool {
    let previous_len = banned_ips.banned_ips.len();
    banned_ips.banned_ips.retain(|entry| entry.ip != ip);
    banned_ips.banned_ips.len() != previous_len
}

fn prune_expired(banned_ips: &mut BannedIpList, now: OffsetDateTime) -> bool {
    let previous_len = banned_ips.banned_ips.len();
    banned_ips
        .banned_ips
        .retain(|entry| entry.expires.is_none_or(|expires| expires >= now));
    banned_ips.banned_ips.len() != previous_len
}

#[cfg(test)]
mod tests {
    use std::net::{IpAddr, Ipv4Addr};

    use pumpkin::data::{banlist_serializer::BannedIpEntry, banned_ip::BannedIpList};
    use time::{Duration, OffsetDateTime};

    use super::{add_ban, parse_ipv4, prune_expired, remove_ban};

    const LOCALHOST: IpAddr = IpAddr::V4(Ipv4Addr::LOCALHOST);

    #[test]
    fn accepts_only_strict_ipv4_addresses() {
        assert_eq!(parse_ipv4(Some("127.0.0.1")).unwrap(), LOCALHOST);
        assert!(parse_ipv4(None).is_err());
        assert!(parse_ipv4(Some("127.00.0.1")).is_err());
        assert!(parse_ipv4(Some("::1")).is_err());
        assert!(parse_ipv4(Some("localhost")).is_err());
    }

    #[test]
    fn add_and_remove_are_idempotent() {
        let mut banned_ips = BannedIpList::default();

        assert!(add_ban(&mut banned_ips, LOCALHOST));
        assert!(!add_ban(&mut banned_ips, LOCALHOST));
        assert_eq!(banned_ips.banned_ips.len(), 1);

        assert!(remove_ban(&mut banned_ips, LOCALHOST));
        assert!(!remove_ban(&mut banned_ips, LOCALHOST));
        assert!(banned_ips.banned_ips.is_empty());
    }

    #[test]
    fn expired_entries_are_removed_before_returning_the_list() {
        let now = OffsetDateTime::now_utc();
        let mut banned_ips = BannedIpList::default();
        banned_ips.banned_ips.push(BannedIpEntry::new(
            LOCALHOST,
            "test".to_owned(),
            Some(now - Duration::SECOND),
            "expired".to_owned(),
        ));

        assert!(prune_expired(&mut banned_ips, now));
        assert!(banned_ips.banned_ips.is_empty());
    }
}
