# No More Useless Keys
[![curseforge downloads](http://cf.way2muchnoise.eu/full_nmuk_downloads.svg)](https://minecraft.curseforge.com/projects/nmuk)
[![curseforge mc versions](http://cf.way2muchnoise.eu/versions/nmuk.svg)](https://minecraft.curseforge.com/projects/nmuk)

![logo](src/main/resources/assets/nmuk/icon.png?raw=true)

## About
This mod allows you to define an arbitrary number of alternative key combinations for every key binding.

![logo](screenshots/screenshot-0.png?raw=true)

## API

If you're a modder you can make use of NMUK's API by including it like this in the `build.gradle`:
```groovy
repositories {
   maven {
       url "https://maven.siphalor.de/"
       name "Siphalor's Maven"
   }
}

dependencies {
   modImplementation "de.siphalor:amecsapi-1.15:1+"
   include "de.siphalor:amecsapi-1.15:1+"
}
```

To register default alternatives see the `NMUKAlternatives` class.
