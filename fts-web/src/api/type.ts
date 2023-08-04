export interface ResponseType extends Promise<any> {
  data?: object;
  code?: number;
  msg?: string;
}
