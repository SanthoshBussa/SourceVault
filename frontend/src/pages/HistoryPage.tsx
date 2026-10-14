import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  History,
  Download,
  Trash2,
  FolderArchive,
  ArrowRight,
  Clock,
  HardDrive,
  CheckCircle2,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  ExternalLink
} from 'lucide-react';
import { listJobs, deleteJob, formatBytes } from '../api';

export const HistoryPage: React.FC = () => {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();
  const [jobToDelete, setJobToDelete] = useState<string | null>(null);

  const { data: jobsData, isLoading } = useQuery({
    queryKey: ['jobsHistory', page],
    queryFn: () => listJobs(page, 10)
  });

  const deleteMutation = useMutation({
    mutationFn: (jobId: string) => deleteJob(jobId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobsHistory'] });
      queryClient.invalidateQueries({ queryKey: ['stats'] });
      setJobToDelete(null);
    }
  });

  return (
    <div className="space-y-6 pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2.5">
            <History className="w-6 h-6 text-brand-700" />
            <span>Collection Job History</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Persisted historical backups stored in local H2 database
          </p>
        </div>

        <Link
          to="/"
          className="px-4 py-2 bg-brand-700 hover:bg-brand-800 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition self-start sm:self-auto"
        >
          <span>New Collection Job</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {/* Table Card */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-500 uppercase font-bold text-[10px] tracking-wider border-b border-slate-200">
              <tr>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Website URL & Name</th>
                <th className="py-3 px-4">Created Date</th>
                <th className="py-3 px-4">Files</th>
                <th className="py-3 px-4">Archive Size</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-400">
                    Loading job history...
                  </td>
                </tr>
              ) : jobsData?.content && jobsData.content.length > 0 ? (
                jobsData.content.map((job) => (
                  <tr key={job.id} className="hover:bg-slate-50/70 transition">
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span
                        className={`text-[10px] font-bold px-2.5 py-0.5 rounded-full ${
                          job.status === 'COMPLETED'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : job.status === 'COMPLETED_WITH_WARNINGS'
                            ? 'bg-amber-50 text-amber-700 border border-amber-200'
                            : job.status === 'COLLECTING' || job.status === 'ARCHIVING'
                            ? 'bg-brand-50 text-brand-700 border border-brand-200 animate-pulse'
                            : job.status === 'CANCELLED'
                            ? 'bg-slate-100 text-slate-600'
                            : 'bg-rose-50 text-rose-700 border border-rose-200'
                        }`}
                      >
                        {job.status}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 max-w-xs truncate">
                      <p className="font-semibold text-slate-900 truncate">{job.websiteUrl}</p>
                      <p className="text-[11px] text-slate-400 font-mono truncate">{job.jobName}</p>
                    </td>
                    <td className="py-3.5 px-4 whitespace-nowrap text-slate-500">
                      {new Date(job.createdAt).toLocaleString()}
                    </td>
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className="font-semibold text-slate-900">{job.totalDownloaded}</span>
                      {job.totalReconstructed > 0 && (
                        <span className="text-violet-600 ml-1 font-medium">
                          (+{job.totalReconstructed} src)
                        </span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 whitespace-nowrap font-mono text-slate-600">
                      {formatBytes(job.archiveSize)}
                    </td>
                    <td className="py-3.5 px-4 whitespace-nowrap text-right">
                      <div className="flex items-center justify-end gap-2">
                        {job.status === 'COLLECTING' || job.status === 'ARCHIVING' ? (
                          <Link
                            to={`/collection/${job.id}`}
                            className="px-2.5 py-1 bg-brand-700 text-white rounded-lg text-xs font-semibold hover:bg-brand-800 transition"
                          >
                            Live
                          </Link>
                        ) : (
                          <>
                            <Link
                              to={`/results/${job.id}`}
                              className="px-2.5 py-1 bg-slate-100 text-slate-700 hover:bg-slate-200 rounded-lg text-xs font-semibold transition"
                            >
                              Inspect
                            </Link>
                            {job.archiveFileName && (
                              <a
                                href={`/api/v1/jobs/${job.id}/archive`}
                                download={job.archiveFileName}
                                title="Download ZIP"
                                className="p-1.5 text-slate-500 hover:text-emerald-700 hover:bg-emerald-50 rounded-lg transition"
                              >
                                <Download className="w-4 h-4" />
                              </a>
                            )}
                          </>
                        )}
                        <button
                          onClick={() => setJobToDelete(job.id)}
                          title="Delete Job"
                          className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-400 space-y-2">
                    <FolderArchive className="w-8 h-8 mx-auto text-slate-300" />
                    <p className="text-xs">No jobs in history yet.</p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {jobsData && jobsData.totalPages > 1 && (
          <div className="flex items-center justify-between p-4 border-t border-slate-100 text-xs text-slate-500">
            <span>
              Page {page + 1} of {jobsData.totalPages} ({jobsData.totalElements} total jobs)
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
                disabled={page >= jobsData.totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="p-1 rounded hover:bg-slate-100 disabled:opacity-40"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {jobToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex items-center gap-3 text-rose-600">
              <div className="w-10 h-10 rounded-xl bg-rose-50 flex items-center justify-center">
                <Trash2 className="w-5 h-5" />
              </div>
              <h3 className="font-bold text-base text-slate-900">Delete Job Archive?</h3>
            </div>
            <p className="text-xs text-slate-600 leading-relaxed">
              This will permanently delete the collection metadata and generated ZIP archive from your local storage.
            </p>
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setJobToDelete(null)}
                className="px-3.5 py-2 bg-slate-100 text-slate-700 hover:bg-slate-200 rounded-lg text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={() => deleteMutation.mutate(jobToDelete)}
                disabled={deleteMutation.isPending}
                className="px-3.5 py-2 bg-rose-600 text-white hover:bg-rose-700 rounded-lg text-xs font-semibold"
              >
                {deleteMutation.isPending ? 'Deleting...' : 'Confirm Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
