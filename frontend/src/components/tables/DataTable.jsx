import { useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import EmptyState from '../common/EmptyState';

// columns: [{ key, header, render?(row), align?: 'right', className? }]
// Pagination is client-side because the backend list endpoints are not paged.
export default function DataTable({
  columns,
  rows,
  rowKey,
  onRowClick,
  pageSize = 10,
  emptyTitle = 'No records found',
  emptyMessage,
}) {
  const [page, setPage] = useState(1);
  const totalPages = Math.max(1, Math.ceil(rows.length / pageSize));

  // Reset/clamp when the data set changes (filters, search, reload).
  useEffect(() => {
    setPage((p) => Math.min(p, totalPages));
  }, [totalPages]);

  useEffect(() => {
    setPage(1);
  }, [rows.length]);

  if (rows.length === 0) return <EmptyState title={emptyTitle} message={emptyMessage} />;

  const start = (page - 1) * pageSize;
  const visible = rows.slice(start, start + pageSize);

  return (
    <>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              {columns.map((c) => (
                <th key={c.key} className={c.align === 'right' ? 'num' : ''}>
                  {c.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {visible.map((row) => (
              <tr
                key={rowKey(row)}
                className={onRowClick ? 'clickable' : ''}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
              >
                {columns.map((c) => (
                  <td key={c.key} className={`${c.align === 'right' ? 'num' : ''} ${c.className || ''}`}>
                    {c.render ? c.render(row) : (row[c.key] ?? '—')}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {rows.length > pageSize && (
        <div className="pagination">
          <span>
            Showing {start + 1}–{Math.min(start + pageSize, rows.length)} of {rows.length}
          </span>
          <div className="row">
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1}
            >
              <ChevronLeft size={14} /> Prev
            </button>
            <span>
              Page {page} of {totalPages}
            </span>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page === totalPages}
            >
              Next <ChevronRight size={14} />
            </button>
          </div>
        </div>
      )}
    </>
  );
}
