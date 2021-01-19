# No More Useless Keys
![logo](src/main/resources/assets/nmuk/icon_large.png?raw=true)

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
