export interface Reader<T> {
  read(): Promise<T>;
  isClosed(): boolean;
  close(): void;
  [Symbol.asyncIterator](): AsyncIterator<T>;
}
