import { useEffect, useMemo, useState } from 'react'
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

        <div className="etab-liste">
          {affichees.map((e) => (
            <article key={e.id} className="etab-carte">
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
              </div>
            </article>
          ))}
        </div>
      </div>
    </div>
  )
}