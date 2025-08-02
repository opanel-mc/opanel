const secondUnit = 1000;
const minuteUnit = secondUnit * 60;
const hourUnit = minuteUnit * 60;

function formatNumber(num: number): string {
  return num < 10 ? `0${num}` : num.toString();
}

/**
 * Transform millisecond to readable time string
 */
export function millisToTime(ms: number): string {
  const hour = Math.floor(ms / hourUnit);
  ms -= hour * hourUnit;
  const minute = Math.floor(ms / minuteUnit);
  ms -= minute * minuteUnit;
  const second = Math.floor(ms / secondUnit);

  return `${formatNumber(hour)}:${formatNumber(minute)}:${formatNumber(second)}`;
}

/**
 * Transform game tick to readable time string
 * @see https://minecraft.fandom.com/wiki/Daylight_cycle
 */
export function gameTickToTime(gt: number): string {
  const hour = Math.floor(gt / 1000 + 6);
  const minute = Math.floor((gt % 1000) / 16.6);
  return `${formatNumber(hour)}:${formatNumber(minute)}`;
}
