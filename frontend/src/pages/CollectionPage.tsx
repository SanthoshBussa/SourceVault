import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Loader2,
  CheckCircle2,
  AlertCircle,
  XCircle,
  Clock,
  HardDrive,
  FileCode,
  Layers,
  StopCircle,
  Search,
  ArrowRight,
  Filter,
  Check,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';
import { getJob, getJobProgress, getJobResources, cancelJob, formatBytes } from '../api';

export const CollectionPage: React.FC = () => {
  const { jobId } = useParams<{ jobId: string }>();
  const navigate = useNavigate();

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [page, setPage] = useState(0);
  const [isCancelling, setIsCancelling] = useState(false);

  // Poll Job Progress every 1000ms
  const { data: progress } = useQuery({
    queryKey: ['jobProgress', jobId],
    queryFn: () => getJobProgress(jobId!),
    enabled: Boolean(jobId),
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data?.status === 'COMPLETED' || data?.status === 'COMPLETED_WITH_WARNINGS' || data?.status === 'FAILED' || data?.status === 'CANCELLED') {
        return false;
      }
      return 1000;
    }
  });

  // Query Job Details
  const { data: job } = useQuery({
    queryKey: ['jobDetails', jobId],
    queryFn: () => getJob(jobId!),
    enabled: Boolean(jobId),
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data?.status === 'COMPLETED' || data?.status === 'COMPLETED_WITH_WARNINGS' || data?.status === 'FAILED' || data?.status === 'CANCELLED') {
        return false;
      }
      return 2000;
    }
  });

  // Query paginated resources
  const { data: resourcesData } = useQuery({
    queryKey: ['jobResources', jobId, statusFilter, typeFilter, search, page],
    queryFn: () => getJobResources(jobId!, { status: statusFilter, type: typeFilter, search, page, size: 15 }),
    enabled: Boolean(jobId),
    refetchInterval: (query) => {
      if (job?.status === 'COMPLETED' || job?.status === 'COMPLETED_WITH_WARNINGS') return false;
      return 3000;
    }
  });

  const isFinished = job?.status === 'COMPLETED' || job?.status === 'COMPLETED_WITH_WARNINGS' ||
                     progress?.status === 'COMPLETED' || progress?.status === 'COMPLETED_WITH_WARNINGS';

  const handleCancel = async () => {
    if (!jobId || isCancelling) return;
    if (!confirm('Are you sure you want to stop this collection job?')) return;

    setIsCancelling(true);
    try {
      await cancelJob(jobId);
    } catch (err: any) {
      alert('Could not cancel job: ' + err.message);
    } finally {
      setIsCancelling(false);
    }
  };

  const currentStatus = job?.status || progress?.status || 'PENDING';
  const percentage = Math.round(progress?.percentage || 0);

  return (
    <div className="space-y-8 pb-16">
      {/* Top Banner & Progress Header */}
      <section className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2.5">
              <span className="font-mono text-xs text-slate-400">ID: {jobId}</span>
              <span
                className={`text-xs font-bold px-2.5 py-0.5 rounded-full ${
                  currentStatus === 'COMPLETED'
                    ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                    : currentStatus === 'COMPLETED_WITH_WARNINGS'
                    ? 'bg-amber-50 text-amber-800 border border-amber-200'
                    : currentStatus === 'COLLECTING' || currentStatus === 'ARCHIVING'
                    ? 'bg-brand-50 text-brand-700 border border-brand-200 animate-pulse'
                    : currentStatus === 'CANCELLED'
                    ? 'bg-slate-100 text-slate-700'
                    : 'bg-rose-50 text-rose-800'
                }`}
              >
                {currentStatus}
              </span>
            </div>
            <h1 className="text-xl sm:text-2xl font-bold text-slate-900 truncate">
              {job?.websiteUrl || 'Collecting Website Assets...'}
            </h1>
          </div>

          <div className="flex items-center gap-3">
            {isFinished ? (
              <Link
                to={`/results/${jobId}`}
                className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm transition"
              >
                <span>View Results & ZIP</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            ) : (
              <button
                onClick={handleCancel}
                disabled={isCancelling || currentStatus === 'CANCELLED'}
                className="px-4 py-2 border border-rose-200 bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition disabled:opacity-50 cursor-pointer"
              >
                <StopCircle className="w-4 h-4" />
                <span>{isCancelling ? 'Cancelling...' : 'Cancel Collection'}</span>
              </button>
            )}
          </div>
        </div>

        {/* Progress Bar */}
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs font-semibold">
            <span className="text-slate-600 flex items-center gap-1.5">
              {!isFinished && <Loader2 className="w-3.5 h-3.5 text-brand-700 animate-spin" />}
              {currentStatus === 'ARCHIVING' ? 'Packaging ZIP Archive & Verifying Integrity...' : 'Downloading Assets & Reconstructing Source Maps...'}
            </span>
            <span className="text-brand-700 font-bold">{percentage}%</span>
          </div>
          <div className="w-full bg-slate-100 h-3 rounded-full overflow-hidden border border-slate-200">
            <div
              className="bg-brand-700 h-full rounded-full transition-all duration-300 ease-out"
              style={{ width: `${percentage}%` }}
            />
          </div>
          <div className="flex items-center justify-between text-[11px] text-slate-400 font-mono">
            <span className="truncate max-w-lg" title={progress?.currentResourceUrl}>
              {progress?.currentResourceUrl ? `Active: ${progress.currentResourceUrl}` : 'Idle'}
            </span>
            <span>Elapsed: {progress?.elapsedSeconds || 0}s</span>
          </div>
        </div>

        {/* Counter Badges */}
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 pt-2">
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-center">
            <p className="text-[10px] uppercase font-bold text-slate-500">Downloaded</p>
            <p className="text-lg font-bold text-emerald-600">{progress?.totalDownloaded || 0}</p>
          </div>
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-center">
            <p className="text-[10px] uppercase font-bold text-slate-500">Failed</p>
            <p className="text-lg font-bold text-rose-600">{progress?.totalFailed || 0}</p>
          </div>
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-center">
            <p className="text-[10px] uppercase font-bold text-slate-500">Skipped</p>
            <p className="text-lg font-bold text-slate-600">{progress?.totalSkipped || 0}</p>
          </div>
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-center">
            <p className="text-[10px] uppercase font-bold text-slate-500">Source Files</p>
            <p className="text-lg font-bold text-violet-600">{job?.totalReconstructed || progress?.totalReconstructed || 0}</p>
          </div>
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-center col-span-2 sm:col-span-1">
            <p className="text-[10px] uppercase font-bold text-slate-500">Total Bytes</p>
            <p className="text-lg font-bold text-slate-900">{formatBytes(progress?.totalBytes || job?.totalBytes)}</p>
          </div>
        </div>
      </section>

      {/* Two Columns: Live Activity Log & Resource Table */}
      <section className="grid lg:grid-cols-3 gap-6">
        {/* Activity Feed */}
        <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <h3 className="font-bold text-slate-900 text-sm flex items-center gap-2">
              <Clock className="w-4 h-4 text-brand-700" />
              <span>Activity Feed</span>
            </h3>
            <span className="text-[10px] font-semibold text-slate-400">Real-time telemetry</span>
          </div>

          <div className="space-y-2 max-h-96 overflow-y-auto text-xs font-mono">
            {progress?.recentEvents && progress.recentEvents.length > 0 ? (
              progress.recentEvents.slice().reverse().map((evt, idx) => (
                <div key={idx} className="p-2 bg-slate-50 rounded-lg text-slate-700 border border-slate-100 leading-tight">
                  {evt}
                </div>
              ))
            ) : (
              <p className="text-xs text-slate-400 py-4 text-center">Awaiting collection events...</p>
            )}
          </div>
        </div>

        {/* Resources Table with Filters */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-200 p-6 shadow-xs space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
            <div>
              <h3 className="font-bold text-slate-900 text-base">Collected Assets & Files</h3>
              <p className="text-xs text-slate-500">Live inventory of inspected items</p>
            </div>

            {/* Filter controls */}
            <div className="flex flex-wrap items-center gap-2">
              <div className="relative">
                <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-2.5" />
                <input
                  type="text"
                  placeholder="Filter URL..."
                  value={search}
                  onChange={(e) => {
                    setSearch(e.target.value);
                    setPage(0);
                  }}
                  className="pl-8 pr-3 py-1.5 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-1 focus:ring-brand-500 w-36 sm:w-44"
                />
              </div>

              <select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
                className="text-xs py-1.5 px-2 rounded-lg border border-slate-200 bg-white text-slate-700 focus:outline-none"
              >
                <option value="">All Statuses</option>
                <option value="DOWNLOADED">Downloaded</option>
                <option value="FAILED">Failed</option>
                <option value="SKIPPED">Skipped</option>
              </select>

              <select
                value={typeFilter}
                onChange={(e) => {
                  setTypeFilter(e.target.value);
                  setPage(0);
                }}
                className="text-xs py-1.5 px-2 rounded-lg border border-slate-200 bg-white text-slate-700 focus:outline-none"
              >
                <option value="">All Types</option>
                <option value="SCRIPT">Scripts</option>
                <option value="STYLESHEET">Stylesheets</option>
                <option value="IMAGE">Images</option>
                <option value="FONT">Fonts</option>
                <option value="SOURCE_MAP">Source Maps</option>
                <option value="HTML">HTML</option>
              </select>
            </div>
          </div>

          {/* Table */}
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 uppercase font-bold text-[10px] tracking-wider border-b border-slate-100">
                <tr>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3">Resource File / URL</th>
                  <th className="py-2.5 px-3">Type</th>
                  <th className="py-2.5 px-3">Size</th>
                  <th className="py-2.5 px-3">Method</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {resourcesData?.content && resourcesData.content.length > 0 ? (
                  resourcesData.content.map((res) => (
                    <tr key={res.id} className="hover:bg-slate-50/70 transition">
                      <td className="py-2.5 px-3 whitespace-nowrap">
                        <span
                          className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                            res.status === 'DOWNLOADED'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : res.status === 'FAILED'
                              ? 'bg-rose-50 text-rose-700 border border-rose-200'
                              : 'bg-slate-100 text-slate-600'
                          }`}
                        >
                          {res.status}
                        </span>
                      </td>
                      <td className="py-2.5 px-3 max-w-xs truncate" title={res.originalUrl}>
                        <p className="font-semibold text-slate-900 truncate">{res.fileName}</p>
                        <p className="text-[11px] text-slate-400 font-mono truncate">{res.originalUrl}</p>
                        {res.failureReason && (
                          <p className="text-[10px] text-rose-600 truncate mt-0.5">{res.failureReason}</p>
                        )}
                      </td>
                      <td className="py-2.5 px-3 whitespace-nowrap">
                        <span className="text-[10px] font-semibold text-brand-700 bg-brand-50 px-1.5 py-0.5 rounded">
                          {res.resourceType}
                        </span>
                      </td>
                      <td className="py-2.5 px-3 whitespace-nowrap font-mono text-slate-600">
                        {formatBytes(res.fileSize)}
                      </td>
                      <td className="py-2.5 px-3 whitespace-nowrap text-[11px] text-slate-500">
                        {res.retrievalMethod}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-slate-400 text-xs">
                      No matching resources found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {resourcesData && resourcesData.totalPages > 1 && (
            <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs text-slate-500">
              <span>
                Page {page + 1} of {resourcesData.totalPages} ({resourcesData.totalElements} items)
              </span>
              <div className="flex items-center gap-1">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="p-1 rounded hover:bg-slate-100 disabled:opacity-40"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  disabled={page >= resourcesData.totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  className="p-1 rounded hover:bg-slate-100 disabled:opacity-40"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      </section>
    </div>
  );
};
