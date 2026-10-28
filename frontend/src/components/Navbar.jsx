import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import Logo from './Logo'

export default function Navbar() {
  const { user, deconnecter } = useAuth()
  const navigate = useNavigate()
  const peutModerer = user?.role === 'MODERATEUR' || user?.role === 'ADMIN_SANTE'

  function sortir() {
    deconnecter()
    navigate('/')
  }

  return (
    <nav className="nav">
      <Link to="/" className="nav-logo">
        <Logo />
        <span className="nav-logo-texte">
          <span className="nav-logo-nom">Xamlé Santé</span>
          <span className="nav-logo-slogan">Votre santé, notre priorité</span>
        </span>
      </Link>

      <div className="nav-liens">
        <NavLink to="/" end>
          Accueil
        </NavLink>
        <NavLink to="/annuaire">Annuaire</NavLink>
        <NavLink to="/sos" className="nav-sos">
          SOS
        </NavLink>
        <NavLink to="/chaleur">Alertes</NavLink>
        <NavLink to="/agenda">Agenda</NavLink>
        {user?.role === 'PATIENT' && <NavLink to="/rendez-vous">Mes RDV</NavLink>}
        {user?.role === 'PROFESSIONNEL' && <NavLink to="/pro" end>Mon profil</NavLink>}
        {user?.role === 'PROFESSIONNEL' && <NavLink to="/pro/agenda">Mes RDV</NavLink>}
        {peutModerer && <NavLink to="/moderation">Modération</NavLink>}
        {user?.role === 'ADMIN_SANTE' && <NavLink to="/admin/campagnes">Campagnes</NavLink>}
        {user?.role === 'ADMIN_SANTE' && (
          <NavLink to="/admin/tableau-de-bord">Tableau de bord</NavLink>
        )}

        <span className="nav-espace" />

        {user ? (
          <>
            <span className="nav-user">{user.prenom}</span>
            <button className="nav-sortie" onClick={sortir}>
              Déconnexion
            </button>
          </>
        ) : (
          <>
            <NavLink to="/connexion">Connexion</NavLink>
            <NavLink to="/inscription" className="nav-inscription">
              Inscription
            </NavLink>
          </>
        )}
      </div>
    </nav>
  )
}