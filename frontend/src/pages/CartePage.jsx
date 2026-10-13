import { useEffect, useState } from 'react'
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
import './CartePage.css'

// Correction des icônes Leaflet avec Vite
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

const ZIGUINCHOR = [12.5833, -16.2719]

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
  const [etablissements, setEtablissements] = useState([])
  const [specialistes, setSpecialistes] = useState([])
  const [erreur, setErreur] = useState('')
  const [message, setMessage] = useState('')

  const [specialite, setSpecialite] = useState('')
  const [langue, setLangue] = useState('')
  const [disponible, setDisponible] = useState(false)
  const [urgences, setUrgences] = useState(false)

  const [position, setPosition] = useState(null)
  const [cible, setCible] = useState(null)

  // Recharge les résultats quand un filtre ou la position change
  useEffect(() => {
    let annule = false
    const t = setTimeout(() => {
      const paramsS = {}
      if (specialite) paramsS.specialite = specialite
      if (langue) paramsS.langue = langue
      if (disponible) paramsS.disponible = true

      const paramsE = {}
      if (urgences) paramsE.urgence = true

      if (position) {
        paramsS.lat = position[0]
        paramsS.lng = position[1]
        paramsE.lat = position[0]
        paramsE.lng = position[1]
      }

      Promise.all([
        api.get('/specialistes', { params: paramsS }),
        api.get('/etablissements', { params: paramsE }),
      ])
        .then(([s, e]) => {
          if (annule) return
          setSpecialistes(s.data)
          setEtablissements(e.data)
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
  }, [specialite, langue, disponible, urgences, position])

  function autourDeMoi() {
    if (!navigator.geolocation) {
      setMessage("La géolocalisation n'est pas disponible sur cet appareil")
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
      () => setMessage('Position refusée ou indisponible'),
      { enableHighAccuracy: true, timeout: 8000 },
    )
  }

  function positionTest() {
    setPosition(ZIGUINCHOR)
    setCible(ZIGUINCHOR)
    setMessage('')
  }

  function effacerPosition() {
    setPosition(null)
    setMessage('')
  }

  return (
    <div style={{ position: 'fixed', inset: 0 }}>
      <div className="panneau">
        <input
          type="text"
          placeholder="Spécialité (ex : pédiatre)"
          value={specialite}
          onChange={(e) => setSpecialite(e.target.value)}
        />
        <input
          type="text"
          placeholder="Langue (ex : wolof)"
          value={langue}
          onChange={(e) => setLangue(e.target.value)}
        />
        <div className="ligne">
          <label>
            <input
              type="checkbox"
              checked={disponible}
              onChange={(e) => setDisponible(e.target.checked)}
            />{' '}
            Disponible
          </label>
          <label>
            <input
              type="checkbox"
              checked={urgences}
              onChange={(e) => setUrgences(e.target.checked)}
            />{' '}
            Urgences
          </label>
        </div>
        <div className="ligne">
          <button className="btn" onClick={autourDeMoi}>
            Autour de moi
          </button>
          <button className="btn secondaire" onClick={positionTest}>
            Centre-ville (test)
          </button>
          {position && (
            <button className="btn secondaire" onClick={effacerPosition}>
              Effacer
            </button>
          )}
        </div>
        {message && <div className="msg">{message}</div>}
        {erreur && <div className="msg">{erreur}</div>}
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

        {etablissements.map((e) => (
          <Marker key={`e-${e.id}`} position={[e.latitude, e.longitude]}>
            <Popup>
              <strong>{e.nom}</strong>
              <br />
              {e.type}
              {e.urgence && ' · Urgences'}
              <br />
              {e.horaires}
              {e.distanceKm != null && (
                <>
                  <br />
                  {e.distanceKm.toFixed(1)} km
                </>
              )}
            </Popup>
          </Marker>
        ))}

        {specialistes.map((s) => (
          <Marker key={`s-${s.id}`} position={[s.latitude, s.longitude]}>
            <Popup>
              <strong>
                Dr {s.prenom} {s.nom}
              </strong>
              <br />
              {s.specialite}
              <br />
              {s.langues}
              <br />
              {s.telephone}
              {s.distanceKm != null && (
                <>
                  <br />
                  {s.distanceKm.toFixed(1)} km
                </>
              )}
            </Popup>
          </Marker>
        ))}
      </MapContainer>

      <div className="resultats">
        {specialistes.length === 0 && (
          <div className="resultat">Aucun spécialiste trouvé</div>
        )}
        {specialistes.map((s) => (
          <div
            key={s.id}
            className="resultat"
            onClick={() => setCible([s.latitude, s.longitude])}
          >
            <strong>
              Dr {s.prenom} {s.nom}
            </strong>{' '}
            · {s.specialite}
            {s.distanceKm != null && (
              <span className="dist"> · {s.distanceKm.toFixed(1)} km</span>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}