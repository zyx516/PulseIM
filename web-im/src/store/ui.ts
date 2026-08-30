import { create } from 'zustand';

type View = 'messages' | 'contacts' | 'settings';
type UiState = { view: View; activeId: string; setView: (view: View) => void; setActiveId: (id: string) => void };

export const useUiStore = create<UiState>((set) => ({
  view: 'messages', activeId: 'ava', setView: (view) => set({ view }), setActiveId: (activeId) => set({ activeId })
}));
