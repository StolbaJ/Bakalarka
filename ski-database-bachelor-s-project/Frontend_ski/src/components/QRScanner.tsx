'use client'

import { useRef, useState, useEffect, useCallback } from 'react'
import { QrCode, X, CheckCircle, AlertCircle } from 'lucide-react'
import jsQR from 'jsqr'

interface QRScannerProps {
  onScan: (result: string) => void
  onClose: () => void
}

const QRScanner: React.FC<QRScannerProps> = ({ onScan, onClose }) => {
  const videoRef = useRef<HTMLVideoElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const scanIntervalRef = useRef<number | null>(null)
  const [isScanning, setIsScanning] = useState(false)
  const [error, setError] = useState('')
  const [lastResult, setLastResult] = useState('')
  const onScanRef = useRef(onScan)
  const onCloseRef = useRef(onClose)
  onScanRef.current = onScan
  onCloseRef.current = onClose

  const stopScanning = useCallback(() => {
    if (streamRef.current === null && scanIntervalRef.current === null) return
    setIsScanning(false)
    if (scanIntervalRef.current !== null) {
      cancelAnimationFrame(scanIntervalRef.current)
      scanIntervalRef.current = null
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop())
      streamRef.current = null
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null
    }
  }, [])

  useEffect(() => {
    // Zkontrolovat, zda jsme na klientovi
    if (typeof window === 'undefined' || !navigator.mediaDevices) {
      setError('Váš prohlížeč nepodporuje přístup k fotoaparátu. Zkuste použít moderní prohlížeč (Chrome, Firefox, Edge).')
      return
    }

    let mounted = true

    const startScanning = async () => {
      if (!mounted || !videoRef.current || !canvasRef.current) return

      try {
        console.log('Starting QR scanner...')
        setIsScanning(true)
        setError('')

        // Získat stream z kamery
        const stream = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: 'environment', // Zadní kamera
            width: { ideal: 1280 },
            height: { ideal: 720 }
          }
        })

        if (!mounted) {
          stream.getTracks().forEach(track => track.stop())
          return
        }

        streamRef.current = stream

        // Nastavit stream na video element
        if (videoRef.current) {
          videoRef.current.srcObject = stream
          
          // Počkat na načtení videa
          await new Promise<void>((resolve, reject) => {
            if (!videoRef.current || !mounted) {
              reject(new Error('Video element not found'))
              return
            }

            const video = videoRef.current!
            const onLoadedMetadata = () => {
              video.removeEventListener('loadedmetadata', onLoadedMetadata)
              video.play()
                .then(() => resolve())
                .catch(reject)
            }
            
            video.addEventListener('loadedmetadata', onLoadedMetadata)
            video.onerror = reject
            
            // Timeout pro případ, že se metadata nenačtou
            setTimeout(() => {
              if (mounted) {
                video.removeEventListener('loadedmetadata', onLoadedMetadata)
                reject(new Error('Timeout waiting for video metadata'))
              }
            }, 5000)
          })
        }

        // Spustit skenování
        const scanFrame = () => {
          if (!mounted || !videoRef.current || !canvasRef.current) {
            return
          }

          const video = videoRef.current
          const canvas = canvasRef.current
          const ctx = canvas.getContext('2d', { willReadFrequently: true })

          if (!ctx || video.readyState !== video.HAVE_ENOUGH_DATA || 
              video.videoWidth === 0 || video.videoHeight === 0) {
            if (mounted) {
              scanIntervalRef.current = requestAnimationFrame(scanFrame)
            }
            return
          }

          // Nastavit velikost canvas
          canvas.width = video.videoWidth
          canvas.height = video.videoHeight

          // Nakreslit video frame na canvas
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height)

          // Získat image data
          const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)

          // Detekovat QR kód
          try {
            const code = jsQR(imageData.data, imageData.width, imageData.height, {
              inversionAttempts: 'dontInvert'
            })

            if (code && mounted) {
              setLastResult(code.data)
              onScanRef.current(code.data)
              stopScanning()
              onCloseRef.current()
              return
            }
          } catch {
            // Ignorovat chyby při detekci
          }

          // Pokračovat ve skenování
          if (mounted) {
            scanIntervalRef.current = requestAnimationFrame(scanFrame)
          }
        }

        // Spustit scan loop
        scanIntervalRef.current = requestAnimationFrame(scanFrame)
        console.log('QR scanner started successfully')

      } catch (err) {
        console.error('Failed to start scanning:', err)
        
        if (mounted) {
          let errorMessage = 'Nepodařilo se spustit skener.'
          
          if (err instanceof Error) {
            if (err.name === 'NotAllowedError' || err.message.includes('Permission denied')) {
              errorMessage = 'Přístup k fotoaparátu byl zamítnut. Zkontrolujte nastavení oprávnění v prohlížeči.'
            } else if (err.name === 'NotFoundError' || err.message.includes('No devices found')) {
              errorMessage = 'Nebyla nalezena žádná kamera.'
            } else if (err.name === 'NotReadableError' || err.message.includes('could not start video source')) {
              errorMessage = 'Kamera je již používána jinou aplikací.'
            } else if (err.name === 'NotSupportedError' || err.message.includes('not supported')) {
              errorMessage = 'Váš prohlížeč nepodporuje přístup k fotoaparátu. Zkuste použít moderní prohlížeč (Chrome, Firefox, Edge) nebo zkontrolujte, zda aplikace běží na HTTPS nebo localhost.'
            } else {
              errorMessage = `Chyba: ${err.message}`
            }
          }
          
          setError(errorMessage)
          setIsScanning(false)
        }
      }
    }

    // Počkat, až bude komponenta připravena
    const timer = setTimeout(() => {
      startScanning()
    }, 100)

    return () => {
      mounted = false
      clearTimeout(timer)
      stopScanning()
    }
  }, [stopScanning])

  const handleClose = () => {
    stopScanning()
    onClose()
  }

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-75 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl p-8 w-full max-w-xl relative">
        <button
          onClick={handleClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 z-10"
        >
          <X className="w-5 h-5" />
        </button>
        
        <h2 className="text-2xl font-bold text-gray-900 mb-4">Skenování QR kódu</h2>
        
        {error && (
          <div className="mb-4 p-3 bg-red-100 border border-red-200 text-red-800 rounded-md flex items-center space-x-2">
            <AlertCircle className="w-5 h-5" />
            <span>{error}</span>
          </div>
        )}
        
        <div className="relative w-full bg-gray-200 rounded-lg overflow-hidden" style={{ minHeight: '300px' }}>
          <video 
            ref={videoRef}
            className="w-full h-full object-cover"
            autoPlay
            playsInline
            muted
            style={{ display: isScanning ? 'block' : 'none' }}
          />
          <canvas 
            ref={canvasRef}
            className="hidden"
            style={{ display: 'none' }}
          />
          {!isScanning && !error && (
            <div className="absolute inset-0 flex items-center justify-center bg-gray-800 bg-opacity-75 text-white">
              <QrCode className="w-12 h-12 animate-pulse" />
              <span className="ml-4 text-lg">Spouštím skener...</span>
            </div>
          )}
          {isScanning && (
            <div className="absolute top-4 left-1/2 transform -translate-x-1/2 bg-black bg-opacity-50 text-white px-4 py-2 rounded-md text-sm">
              Namiřte kameru na QR kód
            </div>
          )}
        </div>
        
        {lastResult && (
          <div className="mt-4 p-3 bg-green-100 border border-green-200 text-green-800 rounded-md flex items-center space-x-2">
            <CheckCircle className="w-5 h-5" />
            <span>Naskenováno: <span className="font-mono">{lastResult}</span></span>
          </div>
        )}
        
        <div className="mt-6 text-center">
          <button
            onClick={handleClose}
            className="px-6 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors"
          >
            Zavřít skener
          </button>
        </div>
      </div>
    </div>
  )
}

export default QRScanner
