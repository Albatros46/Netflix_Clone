export default function AuthLayout({ title, children }) {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>{title}</h1>
        {children}
      </div>
    </div>
  );
}

export function Alert({ type = 'error', children }) {
  if (!children) return null;
  return <div className={`alert alert-${type}`} role={type === 'error' ? 'alert' : 'status'}>{children}</div>;
}
