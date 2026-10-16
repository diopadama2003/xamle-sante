import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Popup,
  Tooltip,
  ZoomControl,
  useMap,
} from 'react-leaflet'
import L from 'leaflet'
import api from '../api'
import { SYMPTOMES } from '../symptomes'
import './CartePage.css'

const ZIGUINCHOR = [12.5833, -16.2719]
const COULEURS = { FAIBLE: '#f1c40f', MODERE: '#e67e22', ELEVE: '#c0392b' }
const LIBELLES = { FAIBLE: 'Faible', MODERE: 'Modéré', ELEVE: 'Élevé' }

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

// Couche de chaleur (plugin leaflet.heat)
function CoucheChaleur({ zones }) {
  const map = useMap()
  useEffect(() => {
    let couche = null
    let annule = false
    window.L = L // leaflet.heat cherche L dans l'espace global
    import('leaflet.heat').then(() => {
      if (annule || !L.heatLayer) return
      const points = zones.map((z) => [z.latitude, z.longitude, Math.min(1, z.total / 10)])
      couche = L.heatLayer(points, { radius: 45, blur: 30, maxZoom: 15 }).addTo(map)
    })
    return () => {
      annule = true
      if (couche) map.removeLayer(couche)
    }
  }, [map, zones])
  return null
}

export default function ChaleurPage() {
  const [zones, setZones] = useState([])
  const [symptome, setSymptome] = useState('')
  const [jours, setJours] = useState(14)
  const [erreur, setErreur] = useState('')

  useEffect(() => {
    const params = { jours }
    if (symptome) params.symptome = symptome
    api
      .get('/signalements/chaleur', { params })
      .then(({ data }) => {
        setZones(data)
        setErreur('')
      })
      .catch(() => setErreur('Impossible de joindre le serveur'))
  }, [symptome, jours])

  return (
    <div style={{ position: 'fixed', top: 52, left: 0, right: 0, bottom: 0 }}>
      <div className="panneau">
        <strong>Carte de chaleur épidémiologique</strong>
        <select value={symptome} onChange={(e) => setSymptome(e.target.value)}>
          <option value="">Tous les symptômes</option>
          {SYMPTOMES.map((s) => (
            <option key={s.code} value={s.code}>
              {s.libelle}
            </option>
          ))}
        </select>
        <select value={jours} onChange={(e) => setJours(Number(e.target.value))}>
          <option value={7}>7 derniers jours</option>
          <option value={14}>14 derniers jours</option>
          <option value={30}>30 derniers jours</option>
        </select>
        <Link className="btn" to="/signaler">
          Signaler un symptôme
        </Link>
        <div className="note">
          Données anonymes, regroupées par quartier. Un quartier n'apparaît qu'à partir de 3
          signalements validés.
        </div>
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
        <TileLayer
          attribution="&copy; contributeurs OpenStreetMap"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <CoucheChaleur zones={zones} />

        {zones.map((z) => (
          <CircleMarker
            key={z.quartier}
            center={[z.latitude, z.longitude]}
            radius={10}
            pathOptions={{
              color: COULEURS[z.niveau],
              fillColor: COULEURS[z.niveau],
              fillOpacity: 0.35,
            }}
          ><Tooltip permanent direction="top" offset={[0, -8]}>
           {z.quartier}
        </Tooltip>
            <Popup>
              <strong>{z.quartier}</strong>
              <br />
              {z.total} signalements · niveau {LIBELLES[z.niveau]}
            </Popup>
          </CircleMarker>
        ))}
      </MapContainer>

      <div className="resultats">
        {zones.length === 0 && <div className="resultat">Aucune zone à signaler</div>}
        {zones.map((z) => (
          <div key={z.quartier} className="resultat">
            <strong>{z.quartier}</strong> · {z.total} signalements ·{' '}
            <span style={{ color: COULEURS[z.niveau], fontWeight: 600 }}>
              {LIBELLES[z.niveau]}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}