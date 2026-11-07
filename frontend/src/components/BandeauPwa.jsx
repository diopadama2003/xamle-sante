import { useEffect, useState } from 'react'

export default function BandeauPwa() {
  const [enLigne, setEnLigne] = useState(navigator.onLine)
  const [invite, setInvite] = useState(null)

  useEffect(() => {
    const on = () => setEnLigne(true)
    const off = () => setEnLigne(false)
    const avantInstall = (e) => {
      e.preventDefault()
      setInvite(e)
    }
    window.addEventListener('online', on)
    window.addEventListener('offline', off)
    window.addEventListener('beforeinstallprompt', avantInstall)
    window.addEventListener('appinstalled', () => setInvite(null))
    return () => {
      window.removeEventListener('online', on)
      window.removeEventListener('offline', off)
      window.removeEventListener('beforeinstallprompt', avantInstall)
    }
  }, [])

  async function installer() {
    invite.prompt()
    await invite.userChoice
    setInvite(null)
  }

  return (
    <>
      {!enLigne && (
        <div className="bandeau-pwa hors-ligne" role="status">
          Mode hors-ligne : les dernières données consultées sont affichées.
        </div>
      )}
      {invite && enLigne && (
        <div className="bandeau-pwa installer" role="status">
          <span>Installer Xamlé Santé sur ton téléphone</span>
          <button onClick={installer}>Installer</button>
          <button className="fermer" onClick={() => setInvite(null)} aria-label="Fermer">
            ✕
          </button>
        </div>
      )}
    </>
  )
}