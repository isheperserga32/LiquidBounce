import { useMemo, useState } from "react";

import { useModules } from "~/features/clickgui/useModules";

import styles from "./modmenu.module.css";
import Tooltip from "~/components/Tooltip";

export default function ModMenu() {
  const { modulesByCategory, toggleModule } = useModules();
  const [activeCategory, setActiveCategory] = useState(
    Object.keys(modulesByCategory)[0]
  );
  const modules = useMemo(
    () => modulesByCategory[activeCategory],
    [modulesByCategory, activeCategory]
  );

  return (
    <div className={styles.wrapper}>
      <div className={styles.container}>
        <ul className={styles.categories}>
          {Object.keys(modulesByCategory).map((category) => (
            <li
              key={category}
              className={styles.category}
              data-active={category === activeCategory}
            >
              <Tooltip text={category}>
                <button onClick={() => setActiveCategory(category)}>
                  <img
                    className={styles.categoryIcon}
                    src={`/icons/${category.toLowerCase()}.svg`}
                    aria-hidden="true"
                  />
                </button>
              </Tooltip>
            </li>
          ))}
        </ul>
        <main className={styles.categoryContainer}>
          <div className={styles.categoryHeader}>
            <h1 className={styles.categoryTitle}>{activeCategory}</h1>
            <input className={styles.searchbar} placeholder="Search" />
          </div>

          <div className={styles.modules}>
            {modules.map((module) => (
              <div
                key={module.name}
                className={styles.module}
                data-enabled={module.enabled}
              >
                <span className={styles.moduleName}>{module.name}</span>
                <span className={styles.moduleDescription}>
                  {module.description}
                </span>
                <button className={styles.settings}>Settings</button>
                <button className={styles.toggle} onClick={() => toggleModule(module.name)}>
                  {module.enabled ? "Enabled" : "Disabled"}
                </button>
              </div>
            ))}
          </div>
        </main>
      </div>
    </div>
  );
}