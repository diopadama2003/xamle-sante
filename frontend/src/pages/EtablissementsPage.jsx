import { useEffect, useMemo, useRef, useState } from 'react'
import { MapContainer, TileLayer, Marker, Popup, CircleMarker, useMap } from 'react-leaflet'
import L from 'leaflet'
import api from '../api'

const ZIGUINCHOR = [12.5833, -16.2719]
const MSG_CENTRE = 'Distances calculées depuis le centre de Ziguinchor.'

const GROUPES = [
  { code: 'TOUS', libelle: 'Tous', types: null },
  { code: 'HOPITAL', libelle: 'Hôpitaux', types: ['HOPITAL'] },
  { code: 'CLINIQUE', libelle: 'Cliniques', types: ['CLINIQUE'] },
  { code: 'DISPENSAIRE', libelle: 'Dispensaires', types: ['DISPENSAIRE', 'CENTRE_SANTE'] },
  { code: 'PHARMACIE', libelle: 'Pharmacies', types: ['PHARMACIE'] },
]

const TYPES = {
  HOPITAL: 'Hôpital',
  CLINIQUE: 'Clinique',
  PHARMACIE: 'Pharmacie',
  DISPENSAIRE: 'Dispensaire',
  CENTRE_SANTE: 'Centre de santé',
}

function normaliser(s) {
  return (s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
}

// Détecte « 24h/24 », « 24/24 » ou « 24h sur 24 » dans le texte des horaires
function ouvert24h(e) {
  return /24\s*h?\s*(\/|sur)\s*24/i.test(e.horaires || '')
}

// Marqueur carré : vert, rouge pour les urgences, agrandi quand il est sélectionné
function iconeEtab(e, actif) {
  return L.divIcon({
    className: `marqueur-etab ${e.urgence ? 'urgence' : ''} ${actif ? 'actif' : ''}`,
    html: '<span>+</span>',
    iconSize: [30, 30],
    iconAnchor: [15, 15],
    popupAnchor: [0, -14],
  })
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

// Cadre la carte sur les résultats, ou sur l'établissement choisi
function AjusterVue({ points, cible }) {
  const map = useMap()
  useEffect(() => {
    if (cible) {
      map.setView(cible, 16)
    } else if (points.length === 1) {
      map.setView(points[0], 15)
    } else if (points.length > 1) {
      map.fitBounds(points, { padding: [30, 30], maxZoom: 16 })
    }
  }, [points, cible, map])
  return null
}

export default function EtablissementsPage() {
  const [liste, setListe] = useState([])
  const [chargement, setChargement] = useState(true)
  const [erreur, setErreur] = useState('')
  const [recherche, setRecherche] = useState('')
  const [groupe, setGroupe] = useState('TOUS')
  const [seul24h, setSeul24h] = useState(false)
  const [position, setPosition] = useState(null)
  const [info, setInfo] = useState(MSG_CENTRE)
  const [localisation, setLocalisation] = useState(false)
  const [selection, setSelection] = useState(null)
  const [cible, setCible] = useState(null)
  const carteRef = useRef(null)

  const reference = position || ZIGUINCHOR

  useEffect(() => {
    let annule = false
    setChargement(true)
    api
      .get('/etablissements', { params: { lat: reference[0], lng: reference[1] } })
      .then(({ data }) => {
        if (annule) return
        setListe(data)
        setErreur('')
      })
      .catch(() => {
        if (!annule) setErreur('Impossible de joindre le serveur')
      })
      .finally(() => {
        if (!annule) setChargement(false)
      })
    return () => {
      annule = true
    }
  }, [reference])

  // Quand on change de recherche ou de filtre, la carte se recadre sur les résultats
  useEffect(() => {
    setCible(null)
  }, [recherche, groupe, seul24h])

  function localiser() {
    if (!navigator.geolocation) {
      setInfo(`Géolocalisation indisponible. ${MSG_CENTRE}`)
      return
    }
    setLocalisation(true)
    setInfo('Localisation en cours...')
    navigator.geolocation.getCurrentPosition(
      (p) => {
        setPosition([p.coords.latitude, p.coords.longitude])
        setInfo('Distances calculées depuis votre position.')
        setLocalisation(false)
      },
      () => {
        setPosition(null)
        setInfo(`Position introuvable : ${MSG_CENTRE.toLowerCase()}`)
        setLocalisation(false)
      },
      { enableHighAccuracy: true, timeout: 8000 },
    )
  }

  function compter(g) {
    return g.types ? liste.filter((e) => g.types.includes(e.type)).length : liste.length
  }

  const affichees = useMemo(() => {
    const q = normaliser(recherche.trim())
    const g = GROUPES.find((x) => x.code === groupe)
    return liste.filter((e) => {
      if (g.types && !g.types.includes(e.type)) return false
      if (seul24h && !ouvert24h(e)) return false
      if (!q) return true
      return normaliser([e.nom, e.quartier, e.adresse, e.services].join(' ')).includes(q)
    })
  }, [liste, recherche, groupe, seul24h])

  const points = useMemo(() => affichees.map((e) => [e.latitude, e.longitude]), [affichees])

  // Fiche → carte : on remonte vers la carte et on la centre sur l'établissement
  function voirSurCarte(e) {
    setSelection(e.id)
    setCible([e.latitude, e.longitude])
    if (carteRef.current) carteRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  // Carte → liste : on fait défiler jusqu'à la fiche
  function voirDansListe(e) {
    setSelection(e.id)
    const fiche = document.getElementById(`etab-${e.id}`)
    if (fiche) fiche.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  return (
    <div className="page">
      <div className="etab-conteneur">
        <h1>Établissements de santé</h1>
        <p className="etab-sous">
          Hôpitaux, cliniques, dispensaires et pharmacies de Ziguinchor : services, horaires et
          itinéraire.
        </p>

        <div className="etab-filtres">
          <input
            type="text"
            placeholder="Rechercher un nom, un quartier, un service"
            value={recherche}
            onChange={(e) => setRecherche(e.target.value)}
          />
          <div className="chips">
            {GROUPES.map((g) => (
              <button
                key={g.code}
                type="button"
                className={`chip ${groupe === g.code ? 'actif' : ''}`}
                onClick={() => setGroupe(g.code)}
              >
                {g.libelle} <span>{compter(g)}</span>
              </button>
            ))}
            <button
              type="button"
              className={`chip ${seul24h ? 'actif' : ''}`}
              onClick={() => setSeul24h(!seul24h)}
            >
              Ouvert 24h/24
            </button>
            <button type="button" className="chip" onClick={localiser} disabled={localisation}>
              {localisation ? 'Localisation...' : 'Me localiser'}
            </button>
          </div>
          <p className="etab-info">{info}</p>
        </div>

        {erreur && <div className="alerte erreur" style={{ marginTop: 12 }}>{erreur}</div>}
        {chargement && !erreur && <div className="etab-vide">Chargement...</div>}
        {!chargement && !erreur && affichees.length === 0 && (
          <div className="etab-vide">Aucun établissement ne correspond à votre recherche.</div>
        )}

        <div className="etab-map" ref={carteRef}>
          <MapContainer
            center={ZIGUINCHOR}
            zoom={13}
            scrollWheelZoom={false}
            style={{ height: '100%', width: '100%' }}
          >
            <AjusterTaille />
            <AjusterVue points={points} cible={cible} />
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
                <Popup>Votre position</Popup>
              </CircleMarker>
            )}

            {affichees.map((e) => (
              <Marker
                key={e.id}
                position={[e.latitude, e.longitude]}
                icon={iconeEtab(e, selection === e.id)}
                eventHandlers={{ click: () => setSelection(e.id) }}
              >
                <Popup>
                  <strong>{e.nom}</strong>
                  <br />
                  {TYPES[e.type]}
                  {e.distanceKm != null && <> · {e.distanceKm.toFixed(1)} km</>}
                  <br />
                  <a
                    target="_blank"
                    rel="noreferrer"
                    href={`https://www.google.com/maps/dir/?api=1&destination=${e.latitude},${e.longitude}`}
                  >
                    Itinéraire
                  </a>
                  {' · '}
                  <a
                    href={`#etab-${e.id}`}
                    onClick={(ev) => {
                      ev.preventDefault()
                      voirDansListe(e)
                    }}
                  >
                    Voir dans la liste
                  </a>
                </Popup>
              </Marker>
            ))}
          </MapContainer>
        </div>

        <div className="etab-liste">
          {affichees.map((e) => (
            <article
              key={e.id}
              id={`etab-${e.id}`}
              className={`etab-carte ${selection === e.id ? 'selectionnee' : ''}`}
            >
              <h3>{e.nom}</h3>
              <div className="etab-badges">
                <span className="badge">{TYPES[e.type]}</span>
                {e.urgence && <span className="badge urgence">Urgences</span>}
                {ouvert24h(e) && <span className="badge">24h/24</span>}
              </div>
              <p>{[e.adresse, e.quartier].filter(Boolean).join(' · ') || 'Adresse non renseignée'}</p>
              {e.horaires && <p>Horaires : {e.horaires}</p>}
              {e.services && <p>Services : {e.services}</p>}
              {e.distanceKm != null && <p className="dist">{e.distanceKm.toFixed(1)} km</p>}
              <div className="actions">
                {e.telephone && (
                  <a className="bouton" href={`tel:${e.telephone.replace(/\s/g, '')}`}>
                    Appeler
                  </a>
                )}
                <a
                  className="bouton secondaire"
                  target="_blank"
                  rel="noreferrer"
                  href={`https://www.google.com/maps/dir/?api=1&destination=${e.latitude},${e.longitude}`}
                >
                  Itinéraire
                </a>
                <button type="button" className="bouton secondaire" onClick={() => voirSurCarte(e)}>
                  Voir sur la carte
                </button>
              </div>
            </article>
          ))}
        </div>
      </div>
    </div>
  )
}