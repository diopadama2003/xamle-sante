import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'

export default function Navbar() {
  const { user, deconnecter } = useAuth()
  const navigate = useNavigate()
  const peutModerer = user?.role === 'MODERATEUR' || user?.role === 'ADMIN_SANTE'

  function sortir() {
    deconnecter()
    navigate('/')
  }

  return (
    <nav className="navbar">
      <Link to="/">Xamlé Santé</Link>
      <div className="navbar-liens">
        <Link to="/">Accueil</Link>
        <Link to="/sos" className="lien-sos">
          SOS
        </Link>
        <Link to="/chaleur">Alertes</Link>
        <Link to="/agenda">Agenda</Link>
        {user?.role === 'PATIENT' && <Link to="/rendez-vous">Mes RDV</Link>}
        {user?.role === 'PROFESSIONNEL' && <Link to="/pro">Mon profil</Link>}
        {user?.role === 'PROFESSIONNEL' && <Link to="/pro/agenda">Mes RDV</Link>}
        {peutModerer && <Link to="/moderation">Modération</Link>}
        {user?.role === 'ADMIN_SANTE' && <Link to="/admin/campagnes">Campagnes</Link>}
        {user ? (
          <>
            <span>{user.prenom}</span>
            <button className="nav-btn" onClick={sortir}>
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