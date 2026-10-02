const ALPHANUMERIC: &[u8; 62] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

pub(crate) fn alphanumeric(length: usize) -> Result<String, getrandom::Error> {
    let mut result = String::with_capacity(length);
    while result.len() < length {
        let mut bytes = [0u8; 64];
        getrandom::fill(&mut bytes)?;
        for byte in bytes {
            // Reject the uneven tail of the byte range to avoid modulo bias.
            if byte < 248 {
                result.push(ALPHANUMERIC[usize::from(byte) % ALPHANUMERIC.len()] as char);
                if result.len() == length {
                    break;
                }
            }
        }
    }
    Ok(result)
}
