/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        darkBg: '#0f172a',
        darkCard: '#1e293b',
        darkBorder: '#334155',
        brandTeal: '#0d9488',
        brandTealLight: '#14b8a6',
        accentGold: '#f59e0b',
        terminalGreen: '#10b981',
      },
    },
  },
  plugins: [],
}
