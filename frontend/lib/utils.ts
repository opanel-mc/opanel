import axios from "axios";
import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"
import { getCookie } from "cookies-next/client";
import { apiUrl } from "./global";
import { APIResponse } from "./types";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export async function sendGetRequest<R>(api: string): Promise<APIResponse<R>> {
  return (await axios.request({
    method: "get",
    url: apiUrl + api,
    headers: { "X-Credential-Token": getCookie("token") }
  })).data as APIResponse<R>;
}

export async function sendPostRequest<R, T = any>(api: string, body?: T): Promise<APIResponse<R>> {
  const data = body ? JSON.stringify(body) : "";
  
  return (await axios.request({
    method: "post",
    maxBodyLength: Infinity,
    url: apiUrl + api,
    headers: { "Content-Type": "text/plain" },
    data
  })).data as APIResponse<R>;
}
