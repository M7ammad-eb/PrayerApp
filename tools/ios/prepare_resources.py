from pathlib import Path
import xml.etree.ElementTree as ET
import json, shutil
root=Path(__file__).resolve().parents[2]
out=root/'iosApp/Generated/Resources'; out.mkdir(parents=True,exist_ok=True)
shutil.copy2(root/'app/src/main/assets/places.db',out/'places.db')
for audio in (root/'app/src/main/res/raw').glob('*.mp3'): shutil.copy2(audio,out/audio.name)
for language,folder in [('en','values'),('ar','values-ar')]:
    strings={e.attrib['name']:''.join(e.itertext()) for e in ET.parse(root/f'app/src/main/res/{folder}/strings.xml').getroot() if e.tag=='string'}
    (out/f'android-catalog-{language}.json').write_text(json.dumps(strings,ensure_ascii=False,indent=2),encoding='utf-8')
    print(language,len(strings),'Android catalogue entries retained for reference; iOS interface uses iOS-specific localization')
# Existing raster brand asset, exported in native asset catalogue format.
asset=out/'Assets.xcassets/AppIcon.appiconset';asset.mkdir(parents=True,exist_ok=True)
# App icon is rendered at 1024 on macOS using sips from the existing high-resolution launcher.
source=root/'app/src/main/res/mipmap-xxxhdpi/salati_launcher.png'
shutil.copy2(source,asset/'Icon.png')
(asset/'Contents.json').write_text(json.dumps({'images':[{'filename':'Icon.png','idiom':'universal','platform':'ios','size':'1024x1024'}],'info':{'author':'xcode','version':1}}))

accent=out/'Assets.xcassets/AccentColor.colorset';accent.mkdir(parents=True,exist_ok=True)
(accent/'Contents.json').write_text(json.dumps({'colors':[{'idiom':'universal','color':{'color-space':'srgb','components':{'red':'0.18','green':'0.39','blue':'0.31','alpha':'1'}}},{'idiom':'universal','appearances':[{'appearance':'luminosity','value':'dark'}],'color':{'color-space':'srgb','components':{'red':'0.55','green':'0.78','blue':'0.67','alpha':'1'}}}],'info':{'author':'xcode','version':1}}))
