import { useState } from "react";

import Combobox, { Option } from "~/components/Combobox";
import Switch from "~/components/Switch";

import SearchBar from "./SearchBar";

type HeaderProps = {
  onSearch: (value: string) => void;
};

export default function Header({ onSearch }: HeaderProps) {
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [types, setTypes] = useState<Option[]>([
    {
      label: "Microsoft",
      value: "microsoft",
    },
    {
      label: "TheAltening",
      value: "altening",
    },
    {
      label: "MCLeaks",
      value: "mcleaks",
    },
    {
      label: "Cracked",
      value: "cracked",
    },
  ]);

  return (
    <div className="h-[92px] bg-black/70 rounded-xl flex items-center space-x-10 px-8">
      <SearchBar onChange={onSearch} />
      <Switch value={favoritesOnly} onChange={setFavoritesOnly}>
        Favorites Only
      </Switch>
      <Combobox
        options={types}
        onToggle={(option) =>
          setTypes((prev) =>
            prev.map((item) =>
              item.value === option.value
                ? { ...item, checked: !item.checked }
                : item
            )
          )
        }
      >
        Type
      </Combobox>
    </div>
  );
}