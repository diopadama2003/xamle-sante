import { Link } from 'react-router-dom'
import Logo from './Logo'

export default function Footer() {
  return (
    <footer className="acc-pied">
      <div className="acc-conteneur acc-pied-grille">
        <div>
          <div className="acc-pied-logo">
            <Logo taille={30} />
            <strong>Xamlé Santé</strong>
          </div>
          <p className="acc-pied-texte">La santé de tous, notre mission à Ziguinchor.</p>
        </div>
        <div>
          <h4>Liens rapides</h4>
          <Link to="/">Accueil</Link>
          <Link to="/annuaire">Annuaire</Link>
          <Link to="/etablissements">Établissements</Link>
          <Link to="/sos">SOS</Link>
          <Link to="/chaleur">Alertes</Link>
          <Link to="/agenda">Agenda</Link>
        </div>
        <div>
          <h4>Services</h4>
          <Link to="/annuaire">Annuaire médical</Link>
          <Link to="/sos">Urgences</Link>
          <Link to="/chaleur">Alertes santé</Link>
          <Link to="/signaler">Signaler un symptôme</Link>
        </div>
        <div>
          <h4>Nous contacter</h4>
          <p className="acc-pied-texte">Ziguinchor, Sénégal</p>
          <p className="acc-pied-texte">Université Assane Seck de Ziguinchor</p>
        </div>
      </div>
      <div className="acc-pied-bas">
        © {new Date().getFullYear()} Xamlé Santé · Projet pédagogique, Licence 3 Ingénierie
        Informatique
      </div>
    </footer>
  )
}