export interface DiscoveredResource {
  url: string;
  resourceType: string;
  origin: string;
  path: string;
  sourceMapUrl?: string;
  inferredSize: number;
  thirdParty: boolean;
}

export interface AnalysisResponse {
  analysisId: string;
  normalizedUrl: string;
  websiteOrigin: string;
  title: string;
  faviconUrl: string;
  pageAccessible: boolean;
  httpStatus: number;
  browserConnected: boolean;
  totalDiscovered: number;
  typeBreakdown: Record<string, number>;
  originBreakdown: Record<string, number>;
  sourceMapCount: number;
  estimatedSizeBytes: number;
  discoveredResources: DiscoveredResource[];
  warnings: string[];
  recommendedMode: 'URL_ONLY' | 'BROWSER_INTEGRATED';
}

export interface JobResponse {
  id: string;
  jobName: string;
  websiteUrl: string;
  websiteOrigin: string;
  collectionMode: string;
  status: string;
  totalDiscovered: number;
  totalDownloaded: number;
  totalFailed: number;
  totalSkipped: number;
  totalReconstructed: number;
  totalBytes: number;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  archiveFileName?: string;
  archivePath?: string;
  archiveSize?: number;
  validationStatus?: string;
  errorMessage?: string;
}

export interface JobProgress {
  jobId: string;
  status: string;
  totalDiscovered: number;
  totalDownloaded: number;
  totalFailed: number;
  totalSkipped: number;
  totalReconstructed: number;
  totalBytes: number;
  percentage: number;
  currentResourceUrl: string;
  elapsedSeconds: number;
  recentEvents: string[];
}

export interface ResourceItem {
  id: string;
  jobId: string;
  originalUrl: string;
  resourceOrigin: string;
  resourcePath: string;
  fileName: string;
  resourceType: string;
  contentType: string;
  httpStatus: number;
  fileSize: number;
  checksum: string;
  retrievalMethod: string;
  status: string;
  failureReason?: string;
  reconstructed: boolean;
  createdAt: string;
}

export interface StatsResponse {
  totalJobs: number;
  totalDownloaded: number;
  totalArchives: number;
  totalStorageBytes: number;
  activeJobs: number;
  browserStatus: {
    connected: boolean;
    activeSessionCount: number;
    activeTabUrl?: string;
    activeTabTitle?: string;
  };
}

const API_BASE = '';

export async function fetchStats(): Promise<StatsResponse> {
  const res = await fetch(`${API_BASE}/api/v1/stats`);
  if (!res.ok) throw new Error('Failed to fetch stats');
  return res.json();
}

export async function analyzeWebsite(url: string, preferBrowser: boolean = false): Promise<AnalysisResponse> {
  const res = await fetch(`${API_BASE}/api/v1/analysis`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ websiteUrl: url, preferBrowser })
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Failed to analyze website');
  }
  return res.json();
}

export async function getAnalysis(analysisId: string): Promise<AnalysisResponse> {
  const res = await fetch(`${API_BASE}/api/v1/analysis/${analysisId}`);
  if (!res.ok) throw new Error('Analysis not found');
  return res.json();
}

export async function createCollectionJob(payload: {
  analysisId?: string;
  websiteUrl: string;
  collectionMode?: string;
  includeThirdParty?: boolean;
  reconstructSourceMaps?: boolean;
  maxResourceCount?: number;
  maxTotalBytes?: number;
}): Promise<JobResponse> {
  const res = await fetch(`${API_BASE}/api/v1/jobs`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Failed to create collection job');
  }
  return res.json();
}

export async function getJob(jobId: string): Promise<JobResponse> {
  const res = await fetch(`${API_BASE}/api/v1/jobs/${jobId}`);
  if (!res.ok) throw new Error('Job not found');
  return res.json();
}

export async function getJobProgress(jobId: string): Promise<JobProgress> {
  const res = await fetch(`${API_BASE}/api/v1/jobs/${jobId}/progress`);
  if (!res.ok) throw new Error('Failed to fetch progress');
  return res.json();
}

export async function getJobResources(
  jobId: string,
  params: { status?: string; type?: string; search?: string; page?: number; size?: number }
): Promise<{ content: ResourceItem[]; totalElements: number; totalPages: number }> {
  const query = new URLSearchParams();
  if (params.status) query.set('status', params.status);
  if (params.type) query.set('type', params.type);
  if (params.search) query.set('search', params.search);
  query.set('page', String(params.page || 0));
  query.set('size', String(params.size || 20));

  const res = await fetch(`${API_BASE}/api/v1/jobs/${jobId}/resources?${query.toString()}`);
  if (!res.ok) throw new Error('Failed to fetch resources');
  return res.json();
}

export async function cancelJob(jobId: string): Promise<void> {
  await fetch(`${API_BASE}/api/v1/jobs/${jobId}/cancel`, { method: 'POST' });
}

export async function listJobs(page = 0, size = 10): Promise<{ content: JobResponse[]; totalElements: number; totalPages: number }> {
  const res = await fetch(`${API_BASE}/api/v1/jobs?page=${page}&size=${size}`);
  if (!res.ok) throw new Error('Failed to fetch jobs');
  return res.json();
}

export async function deleteJob(jobId: string): Promise<void> {
  const res = await fetch(`${API_BASE}/api/v1/jobs/${jobId}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Failed to delete job');
}

export async function getBrowserStatus(): Promise<any> {
  const res = await fetch(`${API_BASE}/api/v1/browser/status`);
  if (!res.ok) return { connected: false };
  return res.json();
}

export function formatBytes(bytes?: number): string {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
}
