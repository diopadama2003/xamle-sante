import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MapContainer, TileLayer, Marker, useMap, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'
import api from '../api'
import { useAuth } from '../AuthContext'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

const TYPES = {
  HOPITAL: 'Hôpital',
  CLINIQUE: 'Clinique',
  PHARMACIE: 'Pharmacie',
  DISPENSAIRE: 'Dispensaire / poste de santé',
  CENTRE_SANTE: 'Centre de santé',
}

const ZIGUINCHOR = [12.5833, -16.2719]

const VIDE = {
  nom: '',
  type: 'HOPITAL',
  adresse: '',
  quartier: '',
  latitude: '',
  longitude: '',
  telephone: '',
  horaires: '',
  services: '',
  urgence: false,
}

function ChoixPosition({ position, onChoix }) {
  useMapEvents({
    click(e) {
      onChoix(e.latlng.lat, e.latlng.lng)
    },
  })
  return position ? <Marker position={position} /> : null
}

function RecentrerSur({ cible }) {
  const map = useMap()
  useEffect(() => {
    if (cible) map.setView(cible, 16)
  }, [cible, map])
  return null
}

export default function AdminEtablissementsPage() {
  const { deconnecter } = useAuth()
  const navigate = useNavigate()
  const [liste, setListe] = useState([])
  const [form, setForm] = useState(VIDE)
  const [idEdition, setIdEdition] = useState(null)
  const [cible, setCible] = useState(null)
  const [message, setMessage] = useState('')
  const [erreur, setErreur] = useState('')
  const [envoi, setEnvoi] = useState(false)

  function sessionExpiree() {
    deconnecter()
    navigate('/connexion')
  }

  function charger() {
    api
      .get('/etablissements')
      .then(({ data }) => setListe(data))
      .catch(() => setErreur('Impossible de joindre le serveur'))
  }

  useEffect(() => {
    charger()
  }, [])

  const lat = parseFloat(form.latitude)
  const lng = parseFloat(form.longitude)
  const position = !Number.isNaN(lat) && !Number.isNaN(lng) ? [lat, lng] : null

  function changer(champ) {
    return (e) => setForm({ ...form, [champ]: e.target.value })
  }

  function choisirPosition(la, lo) {
    setForm((f) => ({ ...f, latitude: la.toFixed(6), longitude: lo.toFixed(6) }))
  }

  function maPosition() {
    if (!navigator.geolocation) {
      setErreur("La géolocalisation n'est pas disponible")
      return
    }
    navigator.geolocation.getCurrentPosition(
      (p) => {
        choisirPosition(p.coords.latitude, p.coords.longitude)
        setCible([p.coords.latitude, p.coords.longitude])
        setErreur('')
      },
      () => setErreur('Position refusée ou indisponible'),
      { enableHighAccuracy: true, timeout: 8000 },
    )
  }

  function remonter() {
    document.querySelector('.page')?.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function modifier(e) {
    setIdEdition(e.id)
    setForm({
      nom: e.nom ?? '',
      type: e.type,
      adresse: e.adresse ?? '',
      quartier: e.quartier ?? '',
      latitude: e.latitude,
      longitude: e.longitude,
      telephone: e.telephone ?? '',
      horaires: e.horaires ?? '',
      services: e.services ?? '',
      urgence: !!e.urgence,
    })
    setCible([e.latitude, e.longitude])
    setMessage('')
    setErreur('')
    remonter()
  }

  function annulerEdition() {
    setIdEdition(null)
    setForm(VIDE)
    setCible(null)
    setErreur('')
  }

  async function soumettre(e) {
    e.preventDefault()
    setErreur('')
    setMessage('')
    if (!position) {
      setErreur('Choisis la position : clique sur la carte, ou saisis la latitude et la longitude')
      return
    }
    const charge = {
      nom: form.nom,
      type: form.type,
      adresse: form.adresse,
      quartier: form.quartier,
      latitude: lat,
      longitude: lng,
      telephone: form.telephone,
      horaires: form.horaires,
      services: form.services,
      urgence: form.urgence,
    }
    setEnvoi(true)
    try {
      if (idEdition) {
        await api.put(`/etablissements/${idEdition}`, charge)
        setMessage('Établissement modifié.')
      } else {
        await api.post('/etablissements', charge)
        setMessage('Établissement ajouté.')
      }
      annulerEdition()
      charger()
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else if (code === 400) setErreur('Vérifie les champs (nom, type, position)')
      else setErreur('Impossible de joindre le serveur')
    } finally {
      setEnvoi(false)
    }
  }

  async function supprimer(e) {
    if (!window.confirm(`Supprimer « ${e.nom} » ?`)) return
    setErreur('')
    setMessage('')
    try {
      await api.delete(`/etablissements/${e.id}`)
      setMessage('Établissement supprimé.')
      if (idEdition === e.id) annulerEdition()
      charger()
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else if (code === 409)
        setErreur('Des spécialistes sont rattachés à cet établissement : modifie-le plutôt que de le supprimer')
      else setErreur('La suppression a échoué')
    }
  }

  return (
    <div className="page">
      <form className="formulaire large" onSubmit={soumettre}>
        <h2>{idEdition ? 'Modifier un établissement' : 'Ajouter un établissement'}</h2>

        <label>
          Nom
          <input required value={form.nom} onChange={changer('nom')} />
        </label>
        <label>
          Type
          <select value={form.type} onChange={changer('type')}>
            {Object.entries(TYPES).map(([code, libelle]) => (
              <option key={code} value={code}>
                {libelle}
              </option>
            ))}
          </select>
        </label>
        <label>
          Adresse
          <input value={form.adresse} onChange={changer('adresse')} />
        </label>
        <label>
          Quartier
          <input value={form.quartier} onChange={changer('quartier')} />
        </label>

        <p className="note">
          Position : clique sur la carte à l'endroit exact du bâtiment (zoome avec +). Vérifie-la
          ensuite sur Google Maps.
        </p>
        <div className="mini-carte">
          <MapContainer
            center={ZIGUINCHOR}
            zoom={13}
            scrollWheelZoom={false}
            style={{ height: '100%', width: '100%' }}
          >
            <TileLayer
              attribution="&copy; contributeurs OpenStreetMap"
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <RecentrerSur cible={cible} />
            <ChoixPosition position={position} onChoix={choisirPosition} />
          </MapContainer>
        </div>
        <div className="deux-colonnes">
          <label>
            Latitude
            <input type="number" step="any" value={form.latitude} onChange={changer('latitude')} />
          </label>
          <label>
            Longitude
            <input type="number" step="any" value={form.longitude} onChange={changer('longitude')} />
          </label>
        </div>
        <button type="button" className="bouton secondaire" onClick={maPosition}>
          Utiliser ma position actuelle
        </button>

        <label>
          Téléphone
          <input value={form.telephone} onChange={changer('telephone')} />
        </label>
        <label>
          Horaires
          <input
            placeholder="Lun-Ven 08h-17h"
            value={form.horaires}
            onChange={changer('horaires')}
          />
        </label>
        <label>
          Services
          <textarea rows={3} value={form.services} onChange={changer('services')} />
        </label>
        <label className="case">
          <input
            type="checkbox"
            checked={form.urgence}
            onChange={(e) => setForm({ ...form, urgence: e.target.checked })}
          />
          Dispose d'un service d'urgences (utilisé par le bouton SOS)
        </label>

        {message && <div className="alerte ok">{message}</div>}
        {erreur && <div className="alerte erreur">{erreur}</div>}

        <button className="bouton" type="submit" disabled={envoi}>
          {idEdition ? 'Enregistrer les modifications' : "Ajouter l'établissement"}
        </button>
        {idEdition && (
          <button type="button" className="bouton secondaire" onClick={annulerEdition}>
            Annuler la modification
          </button>
        )}
      </form>

      <div className="formulaire large" style={{ marginTop: 16 }}>
        <h2>Établissements enregistrés ({liste.length})</h2>
        {liste.length === 0 && <div className="alerte attente">Aucun établissement.</div>}
        {liste.map((e) => (
          <div key={e.id} className="carte-profil">
            <div>
              <strong>{e.nom}</strong> <span className="badge">{TYPES[e.type]}</span>
              {e.urgence && <span className="badge urgence">Urgences</span>}
            </div>
            <div className="detail">
              {e.quartier || 'Quartier non renseigné'} · {e.latitude.toFixed(5)},{' '}
              {e.longitude.toFixed(5)}
            </div>
            <div className="detail">
              {e.telephone || 'Sans téléphone'} · {e.horaires || 'Horaires non renseignés'}
            </div>
            <div className="actions">
              <button type="button" className="bouton" onClick={() => modifier(e)}>
                Modifier
              </button>
              <button type="button" className="bouton danger" onClick={() => supprimer(e)}>
                Supprimer
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}