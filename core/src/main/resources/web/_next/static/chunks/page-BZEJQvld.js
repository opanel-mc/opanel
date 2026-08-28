const __vite__mapDeps=(i,m=__vite__mapDeps,d=(m.f||(m.f=["/_next/static/chunks/monaco-editor-Cg15HfTz.js","/_next/static/chunks/rolldown-runtime-B0Z9INg1.js","/_next/static/chunks/framework-B19KePI3.js","/_next/static/chunks/vinext-lD6L2R5n.js","/_next/static/chunks/editor.api-BU4ZnR-y.js","/_next/static/css/editor.BTnyCxaz.css","/_next/static/chunks/monaco.contribution-CoIMb1dx.js","/_next/static/css/monaco-editor.DQOZ2kR8.css"])))=>i.map(i=>d[i]);
import{a as e}from"./rolldown-runtime-B0Z9INg1.js";import{i as t,r as n}from"./framework-B19KePI3.js";import{Y as r}from"./vinext-lD6L2R5n.js";import{t as i}from"./blocks-CxwEFXQ-.js";import{m as a,t as o}from"./sub-page-CWlBHlev.js";import{t as s}from"./gauge-CqaSbiXt.js";import{d as c}from"./dialog-8N51xT-d.js";import{t as l}from"./scroll-text-CSqwnDYG.js";import{t as u}from"./unplug-DllMUQfC.js";import{t as d}from"./users-DT0WT5zC.js";import{s as f}from"./button-_6t3VSiS.js";import{j as p,n as m,w as h}from"./utils-CaZcPrzo.js";import{t as g}from"./fonts-Cj63ZkSr.js";import{n as _}from"./dist-Db1ECiSg.js";import{t as v}from"./use-loading-done-B-XLmk-F.js";import{t as y}from"./i18n-text-BdEAtIyL.js";import{a as b,l as x,s as S}from"./api-D9chwceb.js";import{t as C}from"./dynamic-DQb6cv2_.js";import{t as w}from"./text-copy-Cvb0BBXd.js";import{n as T,t as E}from"./config-item-COnaSVyx.js";var D=e(t(),1),O=n(),k=C(()=>r(()=>import(`./monaco-editor-Cg15HfTz.js`),__vite__mapDeps([0,1,2,3,4,5,6,7]),import.meta.url),{ssr:!1,loadableGenerated:{modules:[`components/monaco-editor.tsx`]}});function A({interfaceName:e,icon:t,children:n}){let[r,i]=(0,D.useState)(!1),o=(0,D.useCallback)(async()=>{try{let{enabled:t}=await b(`/api/open-api/${e}`);i(t)}catch(t){x(t,`${h(`open-api.fetch.error`)} (${e})`,[[400,h(`common.error.400`)],[401,h(`common.error.401`)],[500,h(`common.error.500`)]])}},[e]),s=async t=>{try{await S(`/api/open-api/${e}?enabled=${t?`1`:`0`}`),i(t)}catch(n){x(n,`${h(t?`open-api.toggle.enable.error`:`open-api.toggle.disable.error`)} (${e})`,[[400,h(`common.error.400`)],[401,h(`common.error.401`)],[500,h(`common.error.500`)]])}};return(0,D.useEffect)(()=>{o()},[o]),(0,O.jsxs)(T,{children:[(0,O.jsx)(E,{icon:t,name:h(`open-api.interfaces.`+e),children:(0,O.jsx)(a,{checked:r,onCheckedChange:e=>s(e)})}),n]})}function j({method:e,route:t,children:n}){return(0,O.jsxs)(`div`,{"data-slot":`interface`,className:`p-3 border-b last:border-b-0 flex flex-col gap-2`,children:[(0,O.jsxs)(`div`,{className:`flex items-center gap-2`,children:[(0,O.jsx)(`span`,{className:m(`px-1 text-sm`,e===`GET`&&`text-emerald-500`,e===`POST`&&`text-blue-500 dark:text-blue-400`,e===`PATCH`&&`text-yellow-700 dark:text-yellow-600`,e===`DELETE`&&`text-destructive`,g.className),children:e}),(0,O.jsx)(w,{text:t,className:`flex-1`})]}),n]})}function M({children:e}){return(0,O.jsx)(`span`,{className:`text-sm text-muted-foreground`,children:e})}function N({def:e}){let{theme:t}=_();return(0,O.jsxs)(`div`,{className:`flex flex-col gap-2`,children:[(0,O.jsx)(`span`,{className:`text-sm font-semibold`,children:h(`open-api.interfaces.request`)}),(0,O.jsx)(k,{defaultLanguage:`typescript`,value:e,theme:t===`dark`?`opanel-theme-dark-default`:`opanel-theme`,options:{minimap:{enabled:!1},lineNumbers:`off`,automaticLayout:!0,tabSize:2,readOnly:!0,contextmenu:!1,showUnused:!1,showDeprecated:!1,scrollbar:{vertical:`hidden`,handleMouseWheel:!1},...p},autoFitHeight:!0,className:`border rounded-md overflow-hidden`})]})}function P({def:e}){let{theme:t}=_();return(0,O.jsxs)(`div`,{className:`flex flex-col gap-2`,children:[(0,O.jsx)(`span`,{className:`text-sm font-semibold`,children:h(`open-api.interfaces.response`)}),(0,O.jsx)(k,{defaultLanguage:`typescript`,value:e,theme:t===`dark`?`opanel-theme-dark-default`:`opanel-theme`,options:{minimap:{enabled:!1},lineNumbers:`off`,automaticLayout:!0,tabSize:2,readOnly:!0,contextmenu:!1,showUnused:!1,showDeprecated:!1,scrollbar:{vertical:`hidden`,handleMouseWheel:!1},...p},autoFitHeight:!0,className:`border rounded-md overflow-hidden`})]})}function F(){let[e,t]=(0,D.useState)(!1),n=async()=>{try{let{enabled:e}=await b(`/api/open-api`);t(e)}catch(e){x(e,h(`open-api.fetch.error`),[[401,h(`common.error.401`)],[500,h(`common.error.500`)]])}},r=async e=>{try{await S(`/api/open-api?enabled=${e?`1`:`0`}`),t(e)}catch(t){x(t,h(e?`open-api.toggle.enable.error`:`open-api.toggle.disable.error`),[[400,h(`common.error.400`)],[401,h(`common.error.401`)],[500,h(`common.error.500`)]])}};return(0,D.useEffect)(()=>{n()},[]),v(),(0,O.jsxs)(o,{title:h(`open-api.title`),description:h(`open-api.description`),category:h(`sidebar.config`),icon:(0,O.jsx)(u,{}),pageClassName:`min-xl:px-64!`,children:[(0,O.jsx)(T,{children:(0,O.jsx)(E,{name:h(`open-api.item.enabled`),children:(0,O.jsx)(a,{checked:e,onCheckedChange:e=>r(e)})})}),e&&(0,O.jsxs)(O.Fragment,{children:[(0,O.jsx)(y,{className:`block text-sm text-muted-foreground mb-4`,id:`open-api.hint`,args:[(0,O.jsx)(f,{href:`https://opanel.cn`,target:`_blank`,rel:`noopener noreferrer`,children:`opanel.cn`},0)]}),(0,O.jsx)(`h2`,{className:`text-lg font-semibold pl-1 mb-3`,children:h(`open-api.interfaces.title`)}),(0,O.jsx)(A,{interfaceName:`info`,icon:c,children:(0,O.jsxs)(j,{method:`GET`,route:`/open-api/info`,children:[(0,O.jsx)(M,{children:h(`open-api.interfaces.info.description`)}),(0,O.jsx)(N,{def:`{}`}),(0,O.jsx)(P,{def:`{
  motd: string
  port: number
  maxPlayerCount: number
  whitelist: boolean
  uptime: number
  ingameTime: number
  system: {
    os: string
    arch: string
    cpuName: string
    cpuCore: number
    cpuThread: number
    memory: number
    jvmMemory: number
    gpus: string[]
    java: string
  }
}`})]})}),(0,O.jsx)(A,{interfaceName:`monitor`,icon:s,children:(0,O.jsxs)(j,{method:`GET`,route:`/open-api/monitor`,children:[(0,O.jsx)(M,{children:h(`open-api.interfaces.monitor.description`)}),(0,O.jsx)(N,{def:`{}`}),(0,O.jsx)(P,{def:`{
  cpu: number
  memory: number
  tps: number
}`})]})}),(0,O.jsx)(A,{interfaceName:`plugins`,icon:i,children:(0,O.jsxs)(j,{method:`GET`,route:`/open-api/plugins`,children:[(0,O.jsx)(M,{children:h(`open-api.interfaces.plugins.description`)}),(0,O.jsx)(N,{def:`{}`}),(0,O.jsx)(P,{def:`{
  plugins: {
    fileName: string
    name: string
    version?: string
    description?: string
    authors: string[]
    website?: string
    icon?: string
    size: number
    enabled: boolean
    loaded: boolean
  }[]
}`})]})}),(0,O.jsxs)(A,{interfaceName:`players`,icon:d,children:[(0,O.jsxs)(j,{method:`GET`,route:`/open-api/players`,children:[(0,O.jsx)(M,{children:h(`open-api.interfaces.players.description`)}),(0,O.jsx)(N,{def:`{}`}),(0,O.jsx)(P,{def:`{
  players: {
    name: string
    uuid: string
    isOnline: boolean
    isBanned: boolean
    gamemode: "adventure" | "creative" | "survival" | "spectator"
    banReason?: string
    ping?: number
  }[]
}`})]}),(0,O.jsxs)(j,{method:`GET`,route:`/open-api/players/{uuid}`,children:[(0,O.jsx)(M,{children:h(`open-api.interfaces.player.description`)}),(0,O.jsx)(N,{def:`{
  uuid: string // path param
}`}),(0,O.jsx)(P,{def:`{
  name: string
  uuid: string
  isOnline: boolean
  isBanned: boolean
  gamemode: "adventure" | "creative" | "survival" | "spectator"
  banReason?: string
  ping?: number
}`})]})]}),(0,O.jsxs)(A,{interfaceName:`logs`,icon:l,children:[(0,O.jsxs)(j,{method:`GET`,route:`/open-api/logs`,children:[(0,O.jsx)(M,{children:h(`open-api.interfaces.logs.description`)}),(0,O.jsx)(N,{def:`{}`}),(0,O.jsx)(P,{def:`{
  logs: string[]
}`})]}),(0,O.jsxs)(j,{method:`GET`,route:`/open-api/logs/{fileName}`,children:[(0,O.jsx)(M,{children:h(`open-api.interfaces.log.description`)}),(0,O.jsx)(N,{def:`{
  fileName: string // path param
}`})]}),(0,O.jsxs)(j,{method:`GET`,route:`/open-api/logs/{fileName}/download`,children:[(0,O.jsx)(M,{children:h(`open-api.interfaces.log-download.description`)}),(0,O.jsx)(N,{def:`{
  fileName: string // path param
}`})]})]})]})]})}export{F as default};