export interface Writer<T> {
  write(data: T): void;
  isClosed(): boolean;
  close(): void;
}
