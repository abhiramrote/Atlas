import { useState } from "react";
import { LogIn, LogOut, ShieldCheck } from "lucide-react";

import { redirectToGoogleLogin } from "../api/auth";
import { useAuth } from "../context/AuthContext";

/**
 * Login/account control in the top bar.
 *
 * Renders three distinct states rather than toggling classes on one
 * element, because loading, signed-out and signed-in have genuinely
 * different content and interactions, not just different styling.
 */
function UserMenu() {
  const { user, loading, isAdmin, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  if (loading) {
    return <div className="user-menu-skeleton" />;
  }

  if (!user) {
    return (
      <button
        type="button"
        className="login-button"
        onClick={redirectToGoogleLogin}
      >
        <LogIn size={16} />
        Sign in with Google
      </button>
    );
  }

  return (
    <div className="user-menu">
      <button
        type="button"
        className="user-menu-trigger"
        onClick={() => setMenuOpen(!menuOpen)}
      >
        {user.avatarUrl ? (
          <img
            src={user.avatarUrl}
            alt=""
            className="user-avatar"
          />
        ) : (
          <div className="user-avatar-fallback">
            {user.displayName?.charAt(0) ?? user.email.charAt(0)}
          </div>
        )}

        <span className="user-name">
          {user.displayName || user.email}
        </span>

        {isAdmin && (
          <span className="admin-badge" title="Administrator">
            <ShieldCheck size={13} />
          </span>
        )}
      </button>

      {menuOpen && (
        <>
          <div
            className="user-menu-backdrop"
            onClick={() => setMenuOpen(false)}
          />

          <div className="user-menu-dropdown">
            <div className="user-menu-header">
              <strong>{user.displayName}</strong>
              <span>{user.email}</span>
            </div>

            <div className="user-menu-role">
              Role: {user.role}
            </div>

            <button
              type="button"
              className="user-menu-logout"
              onClick={logout}
            >
              <LogOut size={15} />
              Sign out
            </button>
          </div>
        </>
      )}
    </div>
  );
}

export default UserMenu;
