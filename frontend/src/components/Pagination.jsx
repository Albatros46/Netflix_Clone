export default function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  return (
    <div className="pagination">
      <button className="btn btn-grey btn-sm" disabled={page === 0} onClick={() => onChange(page - 1)}>‹ Zurück</button>
      <span>Seite {page + 1} von {totalPages}</span>
      <button className="btn btn-grey btn-sm" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Weiter ›</button>
    </div>
  );
}
