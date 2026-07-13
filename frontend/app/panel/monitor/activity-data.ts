import type { ActivityData } from "@/lib/types";

const MIN_ACTIVITY_DATA_LENGTH = 30;

function parseActivityDate(dateText: string | null): Date | null {
  if(!dateText) return null;

  const date = new Date(dateText);
  if(isNaN(date.getTime())) return null;

  date.setHours(0, 0, 0, 0);
  return date;
}

function addDays(date: Date, days: number): Date {
  const nextDate = new Date(date);
  nextDate.setDate(nextDate.getDate() + days);
  return nextDate;
}

function toActivityDateText(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}T00:00:00`;
}

function createEmptyActivity(date: Date): ActivityData {
  return {
    date: date ? toActivityDateText(date) : null,
    players: []
  };
}

export function fillActivityData(activities: ActivityData[]): ActivityData[] {
  const filledActivities: ActivityData[] = [];
  let previousDate: Date | null = null;

  for(const activity of activities) {
    const currentDate = parseActivityDate(activity.date);

    if(previousDate && currentDate) {
      let missingDate = addDays(previousDate, 1);

      while(missingDate < currentDate) {
        filledActivities.push(createEmptyActivity(missingDate));
        missingDate = addDays(missingDate, 1);
      }
    }

    filledActivities.push(activity);
    previousDate = currentDate;
  }

  const firstDate = filledActivities
    .map((activity) => parseActivityDate(activity.date))
    .find((date) => date !== null);
  let paddingDate = firstDate ?? addDays(new Date(), 1);

  while(filledActivities.length < MIN_ACTIVITY_DATA_LENGTH) {
    paddingDate = addDays(paddingDate, -1);
    filledActivities.unshift(createEmptyActivity(paddingDate));
  }

  return filledActivities;
}
