import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet'
import L from 'leaflet'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'
import api from '../api'

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

export default function CartePage() {
  const [etablissements, setEtablissements] = useState([])
  const [specialistes, setSpecialistes] = useState([])
  const [erreur, setErreur] = useState('')

  useEffect(() => {
    Promise.all([api.get('/etablissements'), api.get('/specialistes')])
      .then(([e, s]) => {
        setEtablissements(e.data)
        setSpecialistes(s.data)
      })
      .catch(() => setErreur('Impossible de joindre le serveur'))
  }, [])

  return (
    <div style={{ position: 'fixed', inset: 0 }}>
      {erreur && (
        <div style={{ position: 'absolute', zIndex: 1000, background: '#fee', padding: 8 }}>
          {erreur}
        </div>
      )}
      <MapContainer center={ZIGUINCHOR} zoom={13} style={{ height: '100%', width: '100%' }}>
        <AjusterTaille />
        <TileLayer
          attribution="&copy; contributeurs OpenStreetMap"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {etablissements.map((e) => (
          <Marker key={`e-${e.id}`} position={[e.latitude, e.longitude]}>
            <Popup>
              <strong>{e.nom}</strong>
              <br />
              {e.type}
              {e.urgence && ' · Urgences'}
              <br />
              {e.horaires}
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
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  )
}