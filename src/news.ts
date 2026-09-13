export type NewsItem = {
  id: string;
  date: string;
  tag: 'Product' | 'Studio' | 'Research';
  title: string;
  summary: string;
  productId?: string;
};

export const news: NewsItem[] = [
  {
    id: 'studio-v2',
    date: '13 Sep 2026',
    tag: 'Studio',
    title: 'QureMED Studio moves to a unified product library',
    summary: 'The launcher now separates catalog, installed applications, licenses, updates, news and support into a single Windows control center.',
  },
  {
    id: 'rehaflow-241',
    date: '12 Sep 2026',
    tag: 'Product',
    title: 'RehaFlow 2.4.1 update channel prepared',
    summary: 'This release line focuses on local-network reliability, discharge workflows and clearer Android TLS diagnostics.',
    productId: 'rehaflow',
  },
  {
    id: 'calc-beta',
    date: '10 Sep 2026',
    tag: 'Product',
    title: 'Medical Calculator Pro enters beta',
    summary: 'The beta catalog includes clinical scores, infusion calculations and structured output designed for fast documentation.',
    productId: 'medical-calculator',
  },
  {
    id: 'vision-research',
    date: '8 Sep 2026',
    tag: 'Research',
    title: 'Vision Lab accessibility prototype added to the roadmap',
    summary: 'QureMED is exploring camera, sensor and voice-assisted workflows for experimental accessibility glasses.',
    productId: 'vision-lab',
  },
];
