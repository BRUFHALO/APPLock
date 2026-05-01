import React, { createContext, useContext, useRef, ReactNode } from 'react';

interface CameraContextType {
  capturePhotoRef: React.MutableRefObject<(() => Promise<void>) | null>;
}

const CameraContext = createContext<CameraContextType | undefined>(undefined);

export function CameraProvider({ children }: { children: ReactNode }) {
  const capturePhotoRef = useRef<(() => Promise<void>) | null>(null);

  return (
    <CameraContext.Provider value={{ capturePhotoRef }}>
      {children}
    </CameraContext.Provider>
  );
}

export function useCameraContext() {
  const context = useContext(CameraContext);
  if (!context) {
    throw new Error('useCameraContext must be used within CameraProvider');
  }
  return context;
}
