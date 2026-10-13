import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'

const VIDE = {
  nom: '',
  prenom: '',
  specialite: '',
  quartier: '',
  latitude: '',
  longitude: '',
  telephone: '',
  horaires: '',
  langues: '',
  disponible: true,
}

const LIBELLES = {
  EN_ATTENTE: 'En attente de validation par un modérateur',
  VALIDE: 'Validé : visible publiquement',
  REJETE: 'Rejeté par un modérateur',
}

function versFormulaire(d) {
  return {
    nom: d.nom ?? '',
    prenom: d.prenom ?? '',
    specialite: d.specialite ?? '',
    quartier: d.quartier ?? '',
    latitude: d.latitude ?? '',
    longitude: d.longitude ?? '',
    telephone: d.telephone ?? '',
    horaires: d.horaires ?? '',
    langues: d.langues ?? '',
    disponible: d.disponible ?? true,
  }
}

export default function EspacePro() {
  const { deconnecter } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(VIDE)
  const [existe, setExiste] = useState(false)
  const [statut, setStatut] = useState(null)
  const [chargement, setChargement] = useState(true)
  const [message, setMessage] = useState('')
  const [erreur, setErreur] = useState('')

  function sessionExpiree() {
    deconnecter()
    navigate('/connexion')
  }

  useEffect(() => {
    api
      .get('/professionnel/profil')
      .then(({ data }) => {
        setForm(versFormulaire(data))
        setExiste(true)
        setStatut(data.statut)
      })
      .catch((err) => {
        const code = err.response?.status
        if (code === 401 || code === 403) sessionExpiree()
        else if (code !== 404) setErreur('Impossible de joindre le serveur')
      })
      .finally(() => setChargement(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function changer(champ) {
    return (e) => setForm({ ...form, [champ]: e.target.value })
  }

  function utiliserMaPosition() {
    if (!navigator.geolocation) {
      setErreur("La géolocalisation n'est pas disponible")
      return
    }
    navigator.geolocation.getCurrentPosition(
      (p) => {
        setForm((f) => ({
          ...f,
          latitude: p.coords.latitude.toFixed(6),
          longitude: p.coords.longitude.toFixed(6),
        }))
        setErreur('')
      },
      () => setErreur('Position refusée ou indisponible'),
      { enableHighAccuracy: true, timeout: 8000 },
    )
  }

  async function soumettre(e) {
    e.preventDefault()
    setErreur('')
    setMessage('')
    const charge = {
      ...form,
      latitude: parseFloat(form.latitude),
      longitude: parseFloat(form.longitude),
    }
    try {
      const { data } = existe
        ? await api.put('/professionnel/profil', charge)
        : await api.post('/professionnel/profil', charge)
      setExiste(true)
      setStatut(data.statut)
      setMessage('Profil enregistré. Il sera visible après validation par un modérateur.')
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else if (code === 400) setErreur('Vérifie les champs (latitude et longitude obligatoires)')
      else setErreur('Impossible de joindre le serveur')
    }
  }

  if (chargement) {
    return <div className="page">Chargement...</div>
  }

  return (
    <div className="page">
      <form className="formulaire" onSubmit={soumettre}>
        <h2>Mon profil professionnel</h2>

        {statut ? (
          <div className={`alerte ${statut === 'VALIDE' ? 'ok' : 'attente'}`}>
            Statut : {LIBELLES[statut]}
          </div>
        ) : (
          <div className="alerte attente">
            Tu n'as pas encore de profil. Remplis le formulaire pour en créer un.
          </div>
        )}

        <label>
          Nom
          <input required value={form.nom} onChange={changer('nom')} />
        </label>
        <label>
          Prénom
          <input required value={form.prenom} onChange={changer('prenom')} />
        </label>
        <label>
          Spécialité
          <input required value={form.specialite} onChange={changer('specialite')} />
        </label>
        <label>
          Quartier
          <input value={form.quartier} onChange={changer('quartier')} />
        </label>
        <label>
          Latitude
          <input
            type="number"
            step="any"
            required
            value={form.latitude}
            onChange={changer('latitude')}
          />
        </label>
        <label>
          Longitude
          <input
            type="number"
            step="any"
            required
            value={form.longitude}
            onChange={changer('longitude')}
          />
        </label>
        <button type="button" className="bouton secondaire" onClick={utiliserMaPosition}>
          Utiliser ma position
        </button>
        <label>
          Téléphone
          <input value={form.telephone} onChange={changer('telephone')} />
        </label>
        <label>
          Horaires
          <input
            placeholder="Lun-Ven 09h-17h"
            value={form.horaires}
            onChange={changer('horaires')}
          />
        </label>
        <label>
          Langues (séparées par des virgules)
          <input
            placeholder="Français,Wolof,Diola"
            value={form.langues}
            onChange={changer('langues')}
          />
        </label>
        <label className="case">
          <input
            type="checkbox"
            checked={form.disponible}
            onChange={(e) => setForm({ ...form, disponible: e.target.checked })}
          />
          Disponible
        </label>

        {message && <div className="alerte ok">{message}</div>}
        {erreur && <div className="alerte erreur">{erreur}</div>}

        <button className="bouton" type="submit">
          {existe ? 'Mettre à jour' : 'Créer mon profil'}
        </button>
        {existe && (
          <p className="note">
            Toute modification remet ton profil en attente de validation.
          </p>
        )}
      </form>
    </div>
  )
}