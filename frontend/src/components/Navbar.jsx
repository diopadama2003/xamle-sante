import { Link } from 'react-router-dom'
import { useAuth } from '../AuthContext'

export default function Navbar() {
  const { user, deconnecter } = useAuth()
  const peutModerer = user?.role === 'MODERATEUR' || user?.role === 'ADMIN_SANTE'

  return (
    <nav className="navbar">
      <Link to="/">Xamlé Santé</Link>
      <div className="navbar-liens">
        <Link to="/sos" className="lien-sos">
          SOS
        </Link>
        <Link to="/chaleur">Alertes</Link>
        <Link to="/agenda">Agenda</Link>
        {user?.role === 'PROFESSIONNEL' && <Link to="/pro">Mon profil</Link>}
        {peutModerer && <Link to="/moderation">Modération</Link>}
        {user?.role === 'ADMIN_SANTE' && <Link to="/admin/campagnes">Campagnes</Link>}
        {user ? (
          <>
            <span>{user.prenom}</span>
            <button className="nav-btn" onClick={deconnecter}>
              Déconnexion
            </button>
          </>
        ) : (
          <>
            <Link to="/connexion">Connexion</Link>
            <Link to="/inscription">Inscription</Link>
          </>
        )}
      </div>
    </nav>
  )
}