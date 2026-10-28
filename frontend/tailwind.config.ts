import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: {
          950: "#08090C",
          900: "#0C0E13",
          850: "#10131A",
          800: "#151922",
          700: "#1D222D",
          600: "#2A303D",
          500: "#3A4252",
        },
        chalk: {
          100: "#F1F3F7",
          200: "#D8DCE5",
          300: "#A8AFBE",
          400: "#7C8496",
          500: "#5A6273",
        },
        mint: {
          300: "#6EE7C4",
          400: "#34D8AC",
          500: "#22D3A8",
          600: "#12B08C",
        },
        ember: "#F5A524",
        coral: "#F4736B",
      },
      fontFamily: {
        sans: [
          "-apple-system",
          "BlinkMacSystemFont",
          "PingFang SC",
          "Hiragino Sans GB",
          "Microsoft YaHei",
          "Segoe UI",
          "sans-serif",
        ],
        mono: ["SFMono-Regular", "Menlo", "Consolas", "monospace"],
      },
      boxShadow: {
        card: "0 24px 60px -32px rgba(0, 0, 0, 0.9)",
        pop: "0 18px 48px -18px rgba(0, 0, 0, 0.85)",
      },
      keyframes: {
        rise: {
          "0%": { opacity: "0", transform: "translateY(10px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        sweep: {
          "0%": { transform: "translateX(-100%)" },
          "100%": { transform: "translateX(200%)" },
        },
      },
      animation: {
        rise: "rise 0.35s ease-out both",
        sweep: "sweep 1.4s ease-in-out infinite",
      },
    },
  },
  plugins: [],
};

export default config;
