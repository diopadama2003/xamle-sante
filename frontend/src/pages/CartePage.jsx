import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  CircleMarker,
  ZoomControl,
  useMap,
} from 'react-leaflet'
import L from 'leaflet'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'
import api from '../api'
import Icone from '../components/Icone'
import AvatarMedecin from '../components/AvatarMedecin'
import Etoiles from '../components/Etoiles'
import AvisSection from '../components/AvisSection'
import './CartePage.css'
import '../annuaire.css'

// Correction des icônes Leaflet avec Vite
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

const ZIGUINCHOR = [12.5833, -16.2719]

const norm = (t) =>
  (t || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()

// Valeurs distinctes (sans tenir compte des accents ni de la casse), triées
function uniques(valeurs) {
  const vues = new Map()
  valeurs.forEach((v) => {
    const t = (v || '').trim()
    if (t && !vues.has(norm(t))) vues.set(norm(t), t)
  })
  return [...vues.values()].sort((a, b) => a.localeCompare(b, 'fr'))
}

// Force Leaflet à se remesurer quand la taille de la carte change
function AjusterTaille() {
  const map = useMap()
  useEffect(() => {
    const t = setTimeout(() => map.invalidateSize(), 200)
    const obs = new ResizeObserver(() => map.invalidateSize())
    obs.observe(map.getContainer())
    return () => {
      clearTimeout(t)
      obs.disconnect()
    }
  }, [map])
  return null
}

// Recentre la carte sur une cible
function RecentrerSur({ cible }) {
  const map = useMap()
  useEffect(() => {
    if (cible) map.setView(cible, 15)
  }, [cible, map])
  return null
}

export default function CartePage() {
  const [params] = useSearchParams()
  const [toutes, setToutes] = useState([])
  const [specialistes, setSpecialistes] = useState([])
  const [erreur, setErreur] = useState('')
  const [message, setMessage] = useState('')

  const [specialite, setSpecialite] = useState(params.get('specialite') || '')
  const [langue, setLangue] = useState('')
  const [disponible, setDisponible] = useState(false)

  const [position, setPosition] = useState(null)
  const [cible, setCible] = useState(null)
  const [selection, setSelection] = useState(null)
  const [ouvert, setOuvert] = useState(() => window.innerWidth > 700)
    const [version, setVersion] = useState(0)

  // Liste complète, une seule fois : elle sert à remplir les menus de choix
  useEffect(() => {
    api
      .get('/specialistes')
      .then(({ data }) => setToutes(data))
      .catch(() => {})
  }, [])

  const specialites = useMemo(() => uniques(toutes.map((s) => s.specialite)), [toutes])
  const langues = useMemo(
    () => uniques(toutes.flatMap((s) => (s.langues || '').split(','))),
    [toutes],
  )

  // Une spécialité venue de l'accueil (« pediatre ») est alignée sur le menu (« Pédiatre »)
  useEffect(() => {
    if (!specialite || specialites.length === 0) return
    const exacte = specialites.find((o) => norm(o) === norm(specialite))
    if (exacte && exacte !== specialite) setSpecialite(exacte)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [specialites])

  const optionsSpecialite =
    specialite && !specialites.some((o) => o === specialite)
      ? [specialite, ...specialites]
      : specialites
  const optionsLangue =
    langue && !langues.some((o) => o === langue) ? [langue, ...langues] : langues

  // Recharge les résultats quand un filtre ou la position change
  useEffect(() => {
    let annule = false
    const t = setTimeout(() => {
      const p = {}
      if (specialite) p.specialite = specialite
      if (langue) p.langue = langue
      if (disponible) p.disponible = true
      if (position) {
        p.lat = position[0]
        p.lng = position[1]
      }

      api
        .get('/specialistes', { params: p })
        .then(({ data }) => {
          if (annule) return
          setSpecialistes(data)
          setErreur('')
        })
        .catch(() => {
          if (!annule) setErreur('Impossible de joindre le serveur')
        })
    }, 300)

    return () => {
      annule = true
      clearTimeout(t)
    }
   }, [specialite, langue, disponible, position, version])

  function autourDeMoi() {
    if (!navigator.geolocation) {
      setMessage("La géolocalisation n'est pas disponible sur cet appareil.")
      return
    }
    setMessage('Localisation en cours...')
    navigator.geolocation.getCurrentPosition(
      (p) => {
        const pos = [p.coords.latitude, p.coords.longitude]
        setPosition(pos)
        setCible(pos)
        setMessage('')
      },
      () => setMessage("Position refusée ou indisponible. Essaie « Centre de Ziguinchor »."),
      { enableHighAccuracy: true, timeout: 8000 },
    )
  }

  function centreVille() {
    setPosition(ZIGUINCHOR)
    setCible(ZIGUINCHOR)
    setMessage('')
  }

  function effacerPosition() {
    setPosition(null)
    setMessage('')
  }

  function reinitialiser() {
    setSpecialite('')
    setLangue('')
    setDisponible(false)
  }

   function choisir(s) {
    setSelection(s.id)
    setCible([s.latitude, s.longitude])
    // Sur téléphone, on replie le panneau pour laisser voir la carte
    if (window.innerWidth <= 700) setOuvert(false)
  }

  const filtresActifs = specialite || langue || disponible
  const n = specialistes.length

  return (
    <div className="carte-pleine">
      <div className="ann-lateral">
        <section className="ann-carte">
          <button
            type="button"
            className="ann-entete"
            onClick={() => setOuvert((o) => !o)}
            aria-expanded={ouvert}
          >
            <span className="ann-entete-icone">
              <Icone nom="pin" taille={20} />
            </span>
            <span className="ann-entete-texte">
              <strong>Trouver un médecin</strong>
              <small>Par spécialité, langue ou distance</small>
            </span>
            <span className={`ann-chevron ${ouvert ? 'ouvert' : ''}`}>
              <Icone nom="chevron" taille={18} />
            </span>
          </button>

          {ouvert && (
            <div className="ann-corps">
              <label className="ann-champ">
                <span>Spécialité</span>
                <select value={specialite} onChange={(e) => setSpecialite(e.target.value)}>
                  <option value="">Toutes les spécialités</option>
                  {optionsSpecialite.map((o) => (
                    <option key={o} value={o}>
                      {o}
                    </option>
                  ))}
                </select>
              </label>

              <label className="ann-champ">
                <span>Langue parlée</span>
                <select value={langue} onChange={(e) => setLangue(e.target.value)}>
                  <option value="">Toutes les langues</option>
                  {optionsLangue.map((o) => (
                    <option key={o} value={o}>
                      {o}
                    </option>
                  ))}
                </select>
              </label>

              <label className="ann-case">
                <input
                  type="checkbox"
                  checked={disponible}
                  onChange={(e) => setDisponible(e.target.checked)}
                />
                Médecins disponibles uniquement
              </label>

              <div className="ann-actions">
                <button type="button" className="ann-btn principal" onClick={autourDeMoi}>
                  <Icone nom="pin" taille={18} /> Autour de moi
                </button>
                <button type="button" className="ann-btn contour" onClick={centreVille}>
                  Centre de Ziguinchor
                </button>
              </div>

              {(position || filtresActifs) && (
                <div className="ann-liens">
                  {position && (
                    <button type="button" className="ann-lien" onClick={effacerPosition}>
                      Effacer ma position
                    </button>
                  )}
                  {filtresActifs && (
                    <button type="button" className="ann-lien" onClick={reinitialiser}>
                      Réinitialiser les filtres
                    </button>
                  )}
                </div>
              )}

              {message && <div className="ann-msg">{message}</div>}
              {erreur && <div className="ann-msg erreur">{erreur}</div>}

              <Link className="ann-etab" to="/etablissements">
                <Icone nom="hopital" taille={18} />
                <span>Voir les établissements de santé</span>
                <Icone nom="fleche" taille={16} />
              </Link>
            </div>
          )}
        </section>

        <section className="ann-carte ann-liste">
          <div className="ann-liste-tete">
            <h2>{position ? 'Médecins à proximité' : 'Médecins référencés'}</h2>
            <span className="ann-compteur">
              {n} résultat{n > 1 ? 's' : ''}
            </span>
          </div>

          {n === 0 && (
            <p className="ann-vide">Aucun médecin ne correspond à ces critères.</p>
          )}

          <ul>
            {specialistes.map((s) => (
              <li key={s.id}>
                <button
                  type="button"
                  className={`ann-medecin ${selection === s.id ? 'actif' : ''}`}
                  onClick={() => choisir(s)}
                >
                  <AvatarMedecin medecin={s} />
                  <span className="ann-infos">
                    <strong>
                      Dr {s.prenom} {s.nom}
                    </strong>
                    <span className="ann-spec">{s.specialite}</span>
                     <Etoiles note={s.noteMoyenne} nombre={s.nombreAvis} petit />
                    {(s.quartier || s.etablissementNom) && (
                      <span className="ann-meta">
                        {[s.quartier, s.etablissementNom].filter(Boolean).join(' · ')}
                      </span>
                    )}
                  </span>
                  <span className="ann-droite">
                    {s.distanceKm != null && (
                      <span className="ann-dist">
                        <Icone nom="pin" taille={14} /> {s.distanceKm.toFixed(1)} km
                      </span>
                    )}
                    <span className={`ann-dispo ${s.disponible ? 'oui' : 'non'}`}>
                      {s.disponible ? 'Disponible' : 'Indisponible'}
                    </span>
                  </span>
                </button>

                {selection === s.id && (
                  <div className="ann-detail">
                    {s.langues && <div>Langues : {s.langues}</div>}
                    {s.horaires && <div>Horaires : {s.horaires}</div>}
                    <AvisSection
                      specialisteId={s.id}
                      nombre={s.nombreAvis}
                      onChange={() => setVersion((v) => v + 1)}
                    />
                    <div className="ann-detail-actions">
                      <Link className="ann-btn principal" to={`/rdv/${s.id}`}>
                        Prendre rendez-vous
                      </Link>
                      {s.telephone && (
                        <a className="ann-btn contour" href={`tel:${s.telephone}`}>
                          Appeler
                        </a>
                      )}
                    </div>
                  </div>
                )}
              </li>
            ))}
          </ul>
        </section>
      </div>

      <MapContainer
        center={ZIGUINCHOR}
        zoom={13}
        zoomControl={false}
        style={{ height: '100%', width: '100%' }}
      >
        <ZoomControl position="bottomright" />
        <AjusterTaille />
        <RecentrerSur cible={cible} />
        <TileLayer
          attribution="&copy; contributeurs OpenStreetMap"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {position && (
          <CircleMarker
            center={position}
            radius={9}
            pathOptions={{ color: '#c0392b', fillColor: '#e74c3c', fillOpacity: 0.8 }}
          >
            <Popup>Ta position</Popup>
          </CircleMarker>
        )}

        {specialistes.map((s) => (
          <Marker
            key={`s-${s.id}`}
            position={[s.latitude, s.longitude]}
            eventHandlers={{ click: () => setSelection(s.id) }}
          >
            <Popup>
              <div className="ann-popup">
                <AvatarMedecin medecin={s} taille={44} />
                <div>
                  <strong>
                    Dr {s.prenom} {s.nom}
                  </strong>
                  <br />
                  {s.specialite}
                  {s.distanceKm != null && (
                    <>
                      <br />
                      {s.distanceKm.toFixed(1)} km
                    </>
                  )}
                  <br />
                  <Link to={`/rdv/${s.id}`}>Prendre rendez-vous</Link>
                </div>
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  )
}