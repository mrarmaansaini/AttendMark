module.exports = {
  content: ["./web/index.html", "./web/app.js"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#EEF2FF',
          100: '#E0E7FF',
          500: '#6366F1',
          600: '#4F46E5',
          700: '#4338CA',
          800: '#3730A3',
          900: '#312E81',
        },
        present: '#16A34A',
        absent: '#DC2626',
        late: '#D97706',
        excuse: '#7C3AED',
        whatsapp: '#25D366'
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Roboto', 'sans-serif']
      }
    }
  },
  plugins: []
}
