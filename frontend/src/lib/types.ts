export type JobStatus = "PENDING" | "PROCESSING" | "SUCCEEDED" | "FAILED";

export type JobStage = "QUEUED" | "MATERIAL_CHECK" | "GENERATE" | "FINALIZE" | "DONE";

export interface Me {
  id: string;
  handle: string;
  displayName: string;
  accentColor: string;
}

export interface DemoAccount {
  handle: string;
  displayName: string;
  accentColor: string;
  /** 演示口令，仅用于一键登录 */
  password: string;
}

export interface Template {
  id: string;
  title: string;
  category: string;
  coverUrl: string;
  description: string;
  requiredImageCount: number;
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
}

export interface TemplateListResponse {
  categories: string[];
  templates: Template[];
}

export interface AssetRef {
  id: string;
  url: string;
}

export interface UploadedAsset extends AssetRef {
  originalName: string;
  contentType: string;
  sizeBytes: number;
}

export interface Job {
  id: string;
  templateId: string;
  templateTitle: string;
  status: JobStatus;
  stage: JobStage;
  stageLabel: string;
  progress: number;
  errorStage: string | null;
  errorMessage: string | null;
  resultUrl: string | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  assets: AssetRef[];
}

export interface ApiErrorBody {
  code?: string;
  message?: string;
}
