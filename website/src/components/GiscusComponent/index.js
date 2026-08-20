import React, { useEffect, useRef } from 'react';
import { useColorMode } from '@docusaurus/theme-common';

export default function GiscusComponent() {
  const { colorMode } = useColorMode();
  const containerRef = useRef(null);

  useEffect(() => {
    const script = document.createElement('script');
    script.src = "https://giscus.app/client.js";
    script.setAttribute('data-repo', "zoozooll/AndroidCameraParameters");
    script.setAttribute('data-repo-id', "R_kgDOMmOa9Q"); // Placeholder
    script.setAttribute('data-category', "Announcements");
    script.setAttribute('data-category-id', "DIC_kwDOMmOa9c4CiI6z"); // Placeholder
    script.setAttribute('data-mapping', "pathname");
    script.setAttribute('data-strict', "0");
    script.setAttribute('data-reactions-enabled', "1");
    script.setAttribute('data-emit-metadata', "0");
    script.setAttribute('data-input-position', "top");
    script.setAttribute('data-theme', colorMode === 'dark' ? 'dark' : 'light');
    script.setAttribute('data-lang', "en");
    script.crossOrigin = "anonymous";
    script.async = true;

    if (containerRef.current) {
      containerRef.current.appendChild(script);
    }

    return () => {
      if (containerRef.current) {
        containerRef.current.innerHTML = '';
      }
    };
  }, [colorMode]);

  return (
    <div style={{ marginTop: '2rem' }}>
      <hr />
      <div ref={containerRef} />
    </div>
  );
}
