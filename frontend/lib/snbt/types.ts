export type NbtPathSegment = string | number;
export type NbtPath = NbtPathSegment[];
export type NbtPathInput = NbtPathSegment | NbtPath;

export interface NbtValue {
  text(): string
}
