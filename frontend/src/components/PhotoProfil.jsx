import { useEffect, useRef, useState } from 'react'
import api from '../api'
import Icone from './Icone'
import AvatarMedecin from './AvatarMedecin'
import '../annuaire.css'

const COTE_MAX = 400

// Recadre l'image en carré et la réduit : la photo envoyée pèse quelques dizaines de Ko
async function reduire(fichier) {
  const image = await createImageBitmap(fichier, { imageOrientation: 'from-image' })
  const cote = Math.min(image.width, image.height)
  const sx = (image.width - cote) / 2
  const sy = (image.height - cote) / 2
  const sortie = Math.min(cote, COTE_MAX)
  const canvas = document.createElement('canvas')
  canvas.width = sortie
  canvas.height = sortie
  canvas.getContext('2d').drawImage(image, sx, sy, cote, cote, 0, 0, sortie, sortie)
  if (image.close) image.close()
  return new Promise((resolve, reject) => {
    canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('conversion'))), 'image/jpeg', 0.85)
  })
}

export default function PhotoProfil({ actif, onStatut }) {
  const entree = useRef(null)
  const [profil, setProfil] = useState(null)
  const [envoi, setEnvoi] = useState(false)
  const [message, setMessage] = useState('')
  const [erreur, setErreur] = useState('')

  useEffect(() => {
    if (!actif) return
    api
      .get('/professionnel/profil')
      .then(({ data }) => setProfil(data))
      .catch(() => {})
  }, [actif])

  function messageErreur(err) {
    const code = err.response?.status
    if (code === 413) return 'Photo trop lourde (2 Mo maximum).'
    if (code === 415) return 'Format non accepté. Utilise une photo JPEG, PNG ou WebP.'
    if (code === 404) return "Enregistre d'abord ton profil."
    if (code === 401 || code === 403) return 'Session expirée : reconnecte-toi.'
    return 'Impossible de joindre le serveur.'
  }

  async function choisir(e) {
    const fichier = e.target.files?.[0]
    e.target.value = ''
    if (!fichier) return
    setErreur('')
    setMessage('')
    setEnvoi(true)
    let image
    try {
      image = await reduire(fichier)
    } catch {
      setErreur("Impossible de lire cette image. Essaie une photo JPEG ou PNG.")
      setEnvoi(false)
      return
    }
    try {
      const donnees = new FormData()
      donnees.append('fichier', image, 'profil.jpg')
      const { data } = await api.post('/professionnel/photo', donnees)
      setProfil(data)
      if (onStatut) onStatut(data.statut)
      setMessage('Photo enregistrée. Elle sera visible après validation par un modérateur.')
    } catch (err) {
      setErreur(messageErreur(err))
    } finally {
      setEnvoi(false)
    }
  }

  async function supprimer() {
    setErreur('')
    setMessage('')
    setEnvoi(true)
    try {
      const { data } = await api.delete('/professionnel/photo')
      setProfil(data)
      setMessage('Photo supprimée.')
    } catch (err) {
      setErreur(messageErreur(err))
    } finally {
      setEnvoi(false)
    }
  }

  if (!actif) {
    return (
      <div className="photo-profil">
        <p className="photo-note">
          Enregistre d'abord ton profil, puis tu pourras ajouter ta photo.
        </p>
      </div>
    )
  }

  return (
    <div className="photo-profil">
      <AvatarMedecin
        medecin={profil || { prenom: '', nom: '', photoUrl: null }}
        taille={96}
      />
      <div className="photo-actions">
        <strong>Photo de profil</strong>
        <small>Une photo claire de ton visage inspire confiance aux patients.</small>
        <div className="photo-boutons">
          <button
            type="button"
            className="bouton"
            disabled={envoi}
            onClick={() => entree.current.click()}
          >
            <Icone nom="camera" taille={16} />{' '}
            {envoi ? 'Envoi...' : profil?.photoUrl ? 'Changer la photo' : 'Ajouter une photo'}
          </button>
          {profil?.photoUrl && (
            <button type="button" className="bouton secondaire" disabled={envoi} onClick={supprimer}>
              Supprimer
            </button>
          )}
        </div>
        <input
          ref={entree}
          type="file"
          accept="image/*"
          hidden
          onChange={choisir}
        />
        {message && <div className="alerte ok">{message}</div>}
        {erreur && <div className="alerte erreur">{erreur}</div>}
      </div>
    </div>
  )
}