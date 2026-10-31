// Kleiner fetch-Wrapper: haengt das JWT an, wirft Fehler mit der Meldung des Backends
const TOKEN_KEY = 'nc_token';

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (token) =>
  token ? localStorage.setItem(TOKEN_KEY, token) : localStorage.removeItem(TOKEN_KEY);

// Englische Backend-Meldungen -> deutsche Texte fuer die Oberflaeche
const MESSAGES = {
  'Invalid email or password!': 'E-Mail oder Passwort ist falsch.',
  'Email already exists!': 'Diese E-Mail-Adresse ist bereits registriert.',
  'Email already exists.': 'Diese E-Mail-Adresse ist bereits registriert.',
  'Current password is incorrect!': 'Das aktuelle Passwort ist falsch.',
  'Access denied!': 'Keine Berechtigung.',
  'You cannot delete your own account!': 'Sie können Ihr eigenes Konto nicht löschen.',
  'You cannot deactivate your own account!': 'Sie können Ihr eigenes Konto nicht deaktivieren.',
  'Cannot remove or deactivate the last active admin user.': 'Der letzte aktive Administrator kann nicht entfernt oder deaktiviert werden.',
  'Registration successful! Please check your email to verify your account!': 'Registrierung erfolgreich! Bitte bestätigen Sie Ihre E-Mail-Adresse über den Link in der E-Mail.',
  'Email verified successfully! You can new login.': 'E-Mail-Adresse bestätigt! Sie können sich jetzt anmelden.',
  'Password reset email sent successfully! Please check your inbox.': 'Wir haben Ihnen eine E-Mail zum Zurücksetzen des Passworts gesendet.',
  'Password reset successfully. You can now log in with your new password.': 'Passwort geändert. Sie können sich jetzt mit dem neuen Passwort anmelden.',
  'Password changed successfully...': 'Passwort erfolgreich geändert.',
  'Verification email resent successfully! Please check your inbox.': 'Bestätigungs-E-Mail wurde erneut gesendet.',
  'Email is already verified! You can log in directly.': 'Die E-Mail-Adresse ist bereits bestätigt. Sie können sich direkt anmelden.',
  'File is too large!': 'Die Datei ist zu groß.',
};

export const translate = (message) => {
  if (!message) return message;
  if (MESSAGES[message]) return MESSAGES[message];
  if (message.startsWith('Your account has been deactivated')) return 'Ihr Konto wurde deaktiviert. Bitte wenden Sie sich an den Support.';
  if (message.startsWith('Please verify your email')) return 'Bitte bestätigen Sie zuerst Ihre E-Mail-Adresse (Link in der Bestätigungs-E-Mail).';
  if (message.startsWith('Invalid or expired') || message.includes('expired')) return 'Der Link ist ungültig oder abgelaufen.';
  if (message.startsWith('User not found')) return 'Kein Konto mit dieser E-Mail-Adresse gefunden.';
  return message;
};

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

export async function api(path, { method = 'GET', body, form } = {}) {
  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  let payload;
  if (form) payload = form;
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  const res = await fetch(`/api${path}`, { method, headers, body: payload });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }

  if (!res.ok) {
    if (res.status === 401 && token) onUnauthorized();
    const message = (data && (data.error || data.message)) || `Fehler ${res.status}`;
    throw new ApiError(res.status, translate(message));
  }
  if (data && typeof data.message === 'string') data.message = translate(data.message);
  return data;
}

// Datei-Upload mit Fortschrittsanzeige (fetch kennt keinen Upload-Fortschritt)
export function uploadFile(kind, file, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `/api/files/upload/${kind}`);
    const token = getToken();
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    xhr.upload.onprogress = (e) => e.lengthComputable && onProgress?.(Math.round((e.loaded / e.total) * 100));
    xhr.onload = () => {
      let data = null;
      try { data = JSON.parse(xhr.responseText); } catch { /* leer */ }
      if (xhr.status >= 200 && xhr.status < 300) resolve(data.uuid);
      else reject(new ApiError(xhr.status, translate(data?.error) || `Upload fehlgeschlagen (${xhr.status})`));
    };
    xhr.onerror = () => reject(new ApiError(0, 'Upload fehlgeschlagen.'));
    const form = new FormData();
    form.append('file', file);
    xhr.send(form);
  });
}

export const formatDuration = (minutes) => {
  if (!minutes) return '';
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h ? `${h} Std. ${m} Min.` : `${m} Min.`;
};
