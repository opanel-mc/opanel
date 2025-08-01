import type { APIResponse } from "./types";
import axios, { type AxiosError } from "axios";
import { getCookie } from "cookies-next/client";
import { toast } from "sonner";

export const apiUrl = (
  (process.env.NODE_ENV === "development")
  ? `http://localhost:3000` // dev
  : "" // prod
);

export const wsUrl = (
  (process.env.NODE_ENV === "development" || !globalThis["window"])
  ? `ws://localhost:3000` // dev
  : `ws://${window.location.host}` // prod
);

/** @see https://crafatar.com */
export const avatarUrl = "https://crafatar.com/avatars/";
/** @see https://crafatar.com */
export const skinUrl = "https://crafatar.com/skins/";
/** @see https://crafatar.com */
export const capeUrl = "https://crafatar.com/capes/";

/**
 * Toast error message with specific descriptions based on http status codes.
 * 
 * By default, it will set the description to `e.message`.
 */
export function toastError(e: AxiosError, message: string, descriptions: [number, string][]) {
  for(const [status, description] of descriptions) {
    if(e.status === status) {
      toast.error(message, { description });
      return;
    }
  }
  toast.error(message, { description: e.message });
}

export async function sendGetRequest<R>(route: string): Promise<APIResponse<R>> {
  return (await axios.request({
    method: "get",
    url: apiUrl + route,
    headers: { "X-Credential-Token": getCookie("token") }
  })).data as APIResponse<R>;
}

export async function sendPostRequestWithoutToken<R, T = any>(route: string, body?: T): Promise<APIResponse<R>> {
  const data = body ? JSON.stringify(body) : "";
  
  return (await axios.request({
    method: "post",
    maxBodyLength: Infinity,
    url: apiUrl + route,
    headers: { "Content-Type": "text/plain" },
    data
  })).data as APIResponse<R>;
}

export async function sendPostRequest<R, T = any>(route: string, body?: T): Promise<APIResponse<R>> {
  const data = body ? JSON.stringify(body) : "";
  
  return (await axios.request({
    method: "post",
    maxBodyLength: Infinity,
    url: apiUrl + route,
    headers: { "Content-Type": "text/plain", "X-Credential-Token": getCookie("token") },
    data
  })).data as APIResponse<R>;
}

export async function sendDeleteRequest<T = any>(route: string, body?: T): Promise<APIResponse<never>> {
  const data = body ? JSON.stringify(body) : "";
  
  return (await axios.request({
    method: "delete",
    maxBodyLength: Infinity,
    url: apiUrl + route,
    headers: { "Content-Type": "text/plain", "X-Credential-Token": getCookie("token") },
    data
  })).data as APIResponse<never>;
}

export async function uploadFile(route: string, file: File, onProgress?: (progress: number) => void): Promise<APIResponse<never>> {
  const formData = new FormData();
  formData.append("file", file);

  return (await axios.request({
    method: "post",
    url: apiUrl + route,
    headers: { "Content-Type": "multipart/form-data", "X-Credential-Token": getCookie("token") },
    data: formData,
    onUploadProgress: (e) => onProgress && onProgress(e.progress ?? 0)
  })).data as APIResponse<never>;
}
