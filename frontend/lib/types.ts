export type APIResponse<T> = {
  code: number
  error: string
} & T;
