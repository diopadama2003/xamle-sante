import { Fragment, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import Logo from '../components/Logo'
import Icone from '../components/Icone'

const SERVICES = [
  {
    icone: 'pin',
    titre: 'Annuaire médical',
    texte: 'Trouvez des médecins et des établissements de santé près de chez vous.',
    lien: '/annuaire',
  },
  {
    icone: 'alerte',
    titre: 'Urgences',
    texte: 'Localisez les urgences les plus proches et prévenez un proche par SMS.',
    lien: '/sos',
  },
  {
    icone: 'cloche',
    titre: 'Alertes santé',
    texte: 'Suivez les symptômes signalés par quartier, de façon anonyme.',
    lien: '/chaleur',
  },
  {
    icone: 'calendrier',
    titre: 'Agenda santé',
    texte: 'Campagnes de vaccination et de dépistage, avec rappel par SMS.',
    lien: '/agenda',
  },
  {
    icone: 'video',
    titre: 'Consultation vidéo',
    texte: 'Réservez un créneau et consultez un médecin à distance.',
    lien: '/annuaire',
  },
  {
    icone: 'crayon',
    titre: 'Signaler un symptôme',
    texte: 'Aidez à repérer les épidémies sans donner votre identité.',
    lien: '/signaler',
  },
]

const ETAPES = [
  { titre: 'Recherchez', texte: 'Choisissez une spécialité ou activez « Autour de moi ».' },
  { titre: 'Réservez', texte: 'Prenez un créneau libre et réglez les frais de dossier.' },
  { titre: 'Consultez', texte: 'Rejoignez la consultation vidéo à l’heure du rendez-vous.' },
]

function Illustration() {
  return (
    <svg viewBox="0 0 400 320" className="acc-illustration">
      <defs>
        <pattern id="points" width="12" height="12" patternUnits="userSpaceOnUse">
          <circle cx="2" cy="2" r="1.6" fill="#ffffff" fillOpacity="0.2" />
        </pattern>
      </defs>
      <path
        d="M30 90 Q110 20 220 50 T375 125 Q395 205 320 262 T145 292 Q50 252 30 90Z"
        fill="url(#points)"
      />
      <g transform="translate(240 120)">
        <path
          d="M0 120 C-10 98 -64 66 -64 0 a64 64 0 1 1 128 0 C64 66 10 98 0 120Z"
          fill="#e8f5ef"
        />
        <circle r="44" fill="#fff" stroke="#0b7a5a" strokeWidth="8" />
        <path d="M-6 -24h12v18h18v12h-18v18h-12v-18h-18v-12h18z" fill="#0b7a5a" />
      </g>
      <g fill="#ffffff" fillOpacity="0.22">
        <circle cx="95" cy="110" r="20" />
        <circle cx="330" cy="70" r="18" />
        <circle cx="75" cy="225" r="16" />
        <circle cx="350" cy="235" r="20" />
      </g>
      <g fill="#ffffff">
        <path d="M95 101h4v7h7v4h-7v7h-4v-7h-7v-4h7z" />
        <path d="M330 61h4v7h7v4h-7v7h-4v-7h-7v-4h7z" />
        <circle cx="75" cy="225" r="4" />
        <circle cx="350" cy="235" r="4" />
      </g>
    </svg>
  )
}

export default function HomePage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [recherche, setRecherche] = useState('')

  function chercher(e) {
    e.preventDefault()
    const q = recherche.trim()
    navigate(q ? `/annuaire?specialite=${encodeURIComponent(q)}` : '/annuaire')
  }

  return (
    <div className="page accueil">
           <header className="acc-hero">
        <div className="acc-hero-photo" aria-hidden="true" />
        <div className="acc-conteneur acc-hero-grille">
          <div>
            <h1>Votre santé, plus proche à Ziguinchor</h1>
            <p>
              Trouvez des spécialistes et des établissements de santé, et accédez rapidement aux
              urgences, où que vous soyez.
            </p>

            <form className="acc-recherche" onSubmit={chercher}>
              <Icone nom="pin" />
              <input
                type="text"
                placeholder="Quelle spécialité ? (ex : pédiatre)"
                value={recherche}
                onChange={(e) => setRecherche(e.target.value)}
              />
              <button type="submit">
                <Icone nom="search" taille={18} /> Rechercher
              </button>
            </form>

            <div className="acc-boutons">
              <Link className="acc-btn-blanc" to="/annuaire">
                <Icone nom="liste" taille={18} /> Voir l’annuaire
              </Link>
              <Link className="acc-btn-rouge" to="/sos">
                <Icone nom="alerte" taille={18} /> Urgence SOS
              </Link>
            </div>
          </div>
        </div>
      </header>
      <div className="acc-conteneur">
        <div className="acc-urgences">
          <a className="acc-urgence" href="tel:1515">
            <Icone nom="pouls" taille={26} />
            <span>
              <small>SAMU</small>
              <strong>1515</strong>
            </span>
          </a>
          <a className="acc-urgence" href="tel:18">
            <Icone nom="alerte" taille={26} />
            <span>
              <small>POMPIERS</small>
              <strong>18</strong>
            </span>
          </a>
          <a className="acc-urgence" href="tel:17">
            <Icone nom="bouclier" taille={26} />
            <span>
              <small>POLICE</small>
              <strong>17</strong>
            </span>
          </a>
        </div>
      </div>

      <section className="acc-conteneur acc-section">
        <h2 className="acc-titre">Nos services</h2>
        <p className="acc-sous">Des solutions simples et fiables pour prendre soin de vous.</p>
        <div className="acc-services">
          {SERVICES.map((s) => (
            <Link key={s.titre} className="acc-service" to={s.lien}>
              <div className="acc-service-icone">
                <Icone nom={s.icone} taille={22} />
              </div>
              <h3>{s.titre}</h3>
              <p>{s.texte}</p>
            </Link>
          ))}
        </div>
      </section>

      <section className="acc-conteneur acc-section">
        <h2 className="acc-titre">Comment ça marche</h2>
        <p className="acc-sous">C’est simple, rapide et conçu pour vous.</p>
        <div className="acc-etapes">
          {ETAPES.map((e, i) => (
            <Fragment key={e.titre}>
              {i > 0 && (
                <span className="acc-fleche">
                  <Icone nom="fleche" />
                </span>
              )}
              <div className="acc-etape">
                <div className="acc-etape-num">{i + 1}</div>
                <div>
                  <h3>{e.titre}</h3>
                  <p>{e.texte}</p>
                </div>
              </div>
            </Fragment>
          ))}
        </div>
      </section>

      <section className="acc-bandeau">
        <div className="acc-conteneur acc-bandeau-contenu">
          <span className="acc-bandeau-icone">
            <Icone nom="bouclier" taille={44} />
          </span>
          <div className="acc-bandeau-texte">
            <h2>Xamlé Santé, à vos côtés à chaque étape.</h2>
            <p>
              Une plateforme locale, sécurisée et engagée pour une santé accessible à tous à
              Ziguinchor. Professionnels de santé : publiez votre profil et recevez des patients en
              consultation vidéo.
            </p>
          </div>
          <div className="acc-bandeau-actions">
            {!user && (
              <>
                <Link className="acc-lien-btn" to="/inscription">
                  Créer un compte <Icone nom="fleche" taille={18} />
                </Link>
                <Link className="acc-lien-btn contour" to="/inscription">
                  Je suis professionnel
                </Link>
              </>
            )}
            {user?.role === 'PROFESSIONNEL' && (
              <Link className="acc-lien-btn" to="/pro">
                Gérer mon profil <Icone nom="fleche" taille={18} />
              </Link>
            )}
          </div>
        </div>
      </section>

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
    </div>
  )
}