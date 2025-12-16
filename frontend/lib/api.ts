import type { APIResponse } from "./types";
import axios, { type AxiosError } from "axios";
import { deleteCookie } from "cookies-next/client";
import { toast } from "sonner";

export const apiUrl = (
  (process.env.NODE_ENV === "development")
  ? `http://localhost:3000` // dev
  : "" // prod
);

export const wsUrl = (
  (process.env.NODE_ENV === "development" || !globalThis["window"])
  ? `ws://localhost:3000` // dev
  : `${window.location.protocol === "http:" ? "ws" : "wss"}://${window.location.host}` // prod
);

/**
 * Toast error message with specific descriptions based on http status codes.
 * 
 * By default, it will set the description to `e.message`.
 */
export function toastError(e: AxiosError, message: string, descriptions: [number, string][]) {
  if(e.status === 401 && window.location.pathname !== "/login") {
    deleteCookie("token");
    window.location.href = "/login";
    return;
  }

  for(const [status, description] of descriptions) {
    if(e.status === status) {
      toast.error(message, { description });
      return;
    }
  }
  toast.error(message, { description: e.message });
}

export async function sendGetRequest<R>(route: string, withCredentials = true): Promise<APIResponse<R>> {
  return (await axios.request({
    method: "get",
    url: apiUrl + route,
    withCredentials,
  })).data as APIResponse<R>;
}

export async function sendGetBlobRequest(route: string, withCredentials = true): Promise<Blob> {
  return (await axios.request({
    method: "get",
    url: apiUrl + route,
    withCredentials,
    responseType: "blob"
  })).data;
}

export async function sendPostRequest<R, T = any>(route: string, body?: T, withCredentials = true): Promise<APIResponse<R>> {
  const data = body ? JSON.stringify(body) : "";
  
  return (await axios.request({
    method: "post",
    maxBodyLength: Infinity,
    url: apiUrl + route,
    withCredentials,
    headers: { "Content-Type": "text/plain" },
    data
  })).data as APIResponse<R>;
}

export async function sendDeleteRequest<T = any>(route: string, body?: T, withCredentials = true): Promise<APIResponse<never>> {
  const data = body ? JSON.stringify(body) : "";
  
  return (await axios.request({
    method: "delete",
    maxBodyLength: Infinity,
    url: apiUrl + route,
    withCredentials,
    headers: { "Content-Type": "text/plain" },
    data
  })).data as APIResponse<never>;
}

export async function uploadFile(route: string, file: File, onProgress?: (progress: number) => void, withCredentials = true): Promise<APIResponse<never>> {
  const formData = new FormData();
  formData.append("file", file);

  return (await axios.request({
    method: "post",
    url: apiUrl + route,
    withCredentials,
    headers: { "Content-Type": "multipart/form-data" },
    data: formData,
    onUploadProgress: (e) => onProgress && onProgress(e.progress ?? 0)
  })).data as APIResponse<never>;
}
