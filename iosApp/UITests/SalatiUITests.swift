import XCTest
final class SalatiUITests:XCTestCase {
    override func setUp() {super.setUp();continueAfterFailure=false}
    func testChooseOfflineCityAndNavigate() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingHeading90"];launch(app)
        chooseCity("Damascus",app:app,captureSetup:true)
        XCTAssertTrue(app.tabBars.buttons["Calendar"].waitForExistence(timeout:10))
        capture("Prayer home")
        app.tabBars.buttons["Calendar"].tap();capture("Hijri calendar with dual dates")
        app.buttons["Next month"].tap();app.buttons["calendarToday"].tap()
        XCTAssertTrue(app.buttons["calendarToday"].exists)
        app.tabBars.buttons["Qibla"].tap();capture("Qibla simulated true heading 90 English")
        XCTAssertTrue(app.otherElements["qiblaCompass"].exists)
        app.tabBars.buttons["Settings"].tap()
        _=languageControl(app)
        capture("Settings language follows iPhone")
        languageControl(app).tap()
        let preferences=XCUIApplication(bundleIdentifier:"com.apple.Preferences")
        let opened=NSPredicate {_,_ in preferences.state == .runningForeground}
        expectation(for:opened,evaluatedWith:nil);waitForExpectations(timeout:10)
        XCTAssertTrue(preferences.navigationBars.firstMatch.waitForExistence(timeout:15),preferences.debugDescription)
        settleSystemPresentation()
        captureScreen("Language action opens iOS Settings — simulator destination")
        app.activate()
        // Simulate the OS relaunch with its per-app language preference. Keep all
        // app data; no bespoke in-app setting may override this launch choice.
        for arabic in [true,false,true,false] {
            app.terminate()
            app.launchArguments=["-uiTestingHeading90"]+systemLanguage(arabic)
            launch(app)
            XCTAssertTrue(app.tabBars.firstMatch.waitForExistence(timeout:10))
            assertLanguageDirection(app,arabic:arabic)
            app.tabBars.buttons[arabic ? "الإعدادات":"Settings"].tap()
            _=languageControl(app)
            capture(arabic ? "Arabic Settings from system language":"English Settings from system language")
            app.tabBars.buttons[arabic ? "القبلة":"Qibla"].tap()
            capture(arabic ? "Arabic Qibla physical heading 90":"English Qibla physical heading 90")
            app.tabBars.buttons[arabic ? "الصلاة":"Prayer"].tap()
            let city=app.staticTexts["homeSelectedLocation"]
            XCTAssertTrue(city.waitForExistence(timeout:5))
            XCTAssertEqual(city.label,arabic ? "دمشق":"Damascus")
        }
        app.tabBars.buttons["Settings"].tap()
        reviewAlertJourney(app)
    }
    func testArabicDarkOfflineFlow() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingArabicDark"];launch(app)
        chooseCity("Madinah",app:app,captureSetup:true)
        XCTAssertTrue(app.tabBars.buttons["التقويم"].waitForExistence(timeout:10))
        capture("Arabic dark prayer home")
        let text=app.staticTexts.allElementsBoundByIndex.map(\.label).joined(separator:" ")
        XCTAssertNil(text.range(of:"[٠-٩۰-۹]",options:.regularExpression),"Western digits must remain independent of Arabic UI")
        app.tabBars.buttons["التقويم"].tap();capture("Arabic dark Hijri calendar")
        app.tabBars.buttons["القبلة"].tap();capture("Arabic dark Qibla")
        app.tabBars.buttons["الإعدادات"].tap();_=languageControl(app);capture("Arabic dark Settings Display")
        reviewAlertJourney(app,arabic:true)
    }
    func testWidgetAndActivityRendererPreviews() {
        for arabic in [false,true] {
            let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingShowcase"]+(arabic ? ["-uiTestingArabicDark"] : []);launch(app)
            chooseCity("Madinah",app:app)
            let showcase=app.buttons["presentationShowcase"]
            reveal(showcase,app:app)
            XCTAssertTrue(showcase.waitForExistence(timeout:10));showcase.tap()
            let prefix=arabic ? "Arabic dark renderer preview" : "English light renderer preview"
            app.buttons["actualActivity.start"].tap()
            let activityStatus=app.staticTexts["actualActivity.status"]
            XCTAssertTrue(activityStatus.waitForExistence(timeout:10))
            if activityStatus.label == "System Live Activity preview started" {
                XCUIDevice.shared.press(.home)
                let springboard=XCUIApplication(bundleIdentifier:"com.apple.springboard")
                let systemPrayer=springboard.descendants(matching:.any).matching(NSPredicate(format:"label CONTAINS %@",arabic ? "المغرب":"Maghrib")).firstMatch
                // Initial system rendering is asynchronous. Some simulator versions do not
                // expose Island AX text, so this bounded wait must not assert availability.
                let prayerTextVisible=systemPrayer.waitForExistence(timeout:15)
                settleSystemPresentation()
                captureScreen(prefix+" actual system compact Live Activity attempt — "+(prayerTextVisible ? "prayer text detected":"system prayer text unavailable"))
                // This operates the system island; it does not impose any app layout geometry.
                springboard.coordinate(withNormalizedOffset:CGVector(dx:0.5,dy:0.035)).press(forDuration:1)
                settleSystemPresentation()
                captureScreen(prefix+" actual system island after expansion gesture")
                app.activate()
                app.buttons["actualActivity.cleanup"].tap()
            } else {capture(prefix+" actual system activity unavailable status")}

            for id in ["small","medium","large","horizontalMedium","horizontalLarge","overnightStandard","overnightHorizontal","midnightStandard","midnightHorizontal","inline","circular","rectangular","activity","activityExpired","compact","minimal","expanded"] {
                let open=app.buttons["open.preview."+id]
                reveal(open,app:app)
                XCTAssertTrue(open.waitForExistence(timeout:5),"Missing isolated preview \(id)")
                open.tap()
                XCTAssertTrue(identified("rendererPreviewDetail",in:app).waitForExistence(timeout:5))
                capture(prefix+" "+id+" isolated")
                app.navigationBars.buttons.element(boundBy:0).tap()
            }
            app.terminate()
        }
    }
    func testSystemActivityCloseAndExpiryRecovery() {
        let app=XCUIApplication()
        app.launchArguments=["-uiTestingReset","-uiTestingShowcase","-uiTestingActivityDuration","180"]
        launch(app);chooseCity("Madinah",app:app)
        let showcase=app.buttons["presentationShowcase"];reveal(showcase,app:app);showcase.tap()
        let springboard=XCUIApplication(bundleIdentifier:"com.apple.springboard")
        app.buttons["actualActivity.start"].tap()
        XCTAssertTrue(app.staticTexts["System Live Activity preview started"].waitForExistence(timeout:10))
        XCUIDevice.shared.press(.home)
        let visiblePrayer=springboard.descendants(matching:.any).matching(NSPredicate(format:"label CONTAINS 'Maghrib'")).firstMatch
        _=visiblePrayer.waitForExistence(timeout:15);settleSystemPresentation()
        springboard.coordinate(withNormalizedOffset:CGVector(dx:0.5,dy:0.035)).press(forDuration:1)
        let close=springboard.buttons["liveActivity.close"]
        XCTAssertTrue(close.waitForExistence(timeout:10),"Actual expanded Activity must offer a working Close action")
        captureScreen("System expanded Activity with Close action")
        close.tap()
        assertSystemActivityDismissed(close:close,prayer:visiblePrayer,app:app)
        captureScreen("System Island after explicit Close")
        app.activate();app.buttons["actualActivity.inspect"].tap()
        XCTAssertEqual(app.staticTexts["actualActivity.status"].label,"Preview activities: 0")

        // A separate short fixture tests expiry. The first deadline must never
        // overlap the Close assertion and masquerade as a working interaction.
        app.terminate()
        app.launchArguments=["-uiTestingShowcase","-uiTestingActivityDuration","40"]+systemLanguage(false)
        launch(app)
        XCTAssertTrue(app.tabBars.firstMatch.waitForExistence(timeout:10))
        reveal(showcase,app:app);showcase.tap()
        app.buttons["actualActivity.start"].tap()
        XCTAssertTrue(app.staticTexts["System Live Activity preview started"].waitForExistence(timeout:10))
        XCUIDevice.shared.press(.home);_=visiblePrayer.waitForExistence(timeout:15);app.terminate()
        RunLoop.current.run(until:Date().addingTimeInterval(42))
        springboard.coordinate(withNormalizedOffset:CGVector(dx:0.5,dy:0.035)).press(forDuration:1)
        settleSystemPresentation();captureScreen("System Activity after deadline with app terminated")
        // A terminated process cannot run a local end timer. If iOS keeps the
        // stale activity visible, its real Close intent must still recover it.
        if close.waitForExistence(timeout:3) {
            close.tap()
            assertSystemActivityDismissed(close:close,prayer:visiblePrayer,app:app)
            captureScreen("Expired system Activity closed without opening Salati")
        }
        app.launchArguments=["-uiTestingShowcase"]+systemLanguage(false);launch(app)
        XCTAssertTrue(app.tabBars.firstMatch.waitForExistence(timeout:10))
        reveal(showcase,app:app);showcase.tap();app.buttons["actualActivity.inspect"].tap()
        XCTAssertEqual(app.staticTexts["actualActivity.status"].label,"Preview activities: 0","Launch cleanup must not depend on a schedule refresh")
        capture("Activity expiry recovery has zero remaining previews")
    }
    func testSystemWidgetGalleryAndInstalledWidget() {
        // Start in Arabic on a fresh simulator. WidgetKit caches gallery previews
        // across app-language changes; the prior run captured English first.
        for arabic in [true,false] {
            let app=XCUIApplication();app.launchArguments=["-uiTestingReset"]+(arabic ? ["-uiTestingArabicDark"]:[])
            launch(app);chooseCity("Madinah",app:app)
            XCUIDevice.shared.press(.home)
            let board=XCUIApplication(bundleIdentifier:"com.apple.springboard")
            board.coordinate(withNormalizedOffset:CGVector(dx:0.5,dy:0.72)).press(forDuration:2)
            let edit=board.buttons["Edit"]
            XCTAssertTrue(edit.waitForExistence(timeout:8),board.debugDescription);edit.tap()
            let add=board.buttons["Add Widget"]
            XCTAssertTrue(add.waitForExistence(timeout:5),board.debugDescription);add.tap()
            let search=board.searchFields.firstMatch
            XCTAssertTrue(search.waitForExistence(timeout:8),board.debugDescription);search.tap();search.typeText("Salati")
            let salati=board.cells.matching(NSPredicate(format:"label ==[c] 'Salati'")).firstMatch
            XCTAssertTrue(salati.waitForExistence(timeout:10),board.debugDescription);salati.tap()
            let prefix=arabic ? "Arabic actual WidgetKit":"English actual WidgetKit"
            for page in 0..<5 {
                // The remote gallery's first timeline can arrive after its sheet
                // animation. Capture populated content, not the loading skeleton.
                RunLoop.current.run(until:Date().addingTimeInterval(page == 0 ? 8:3))
                captureScreen(prefix+" gallery page \(page+1) live archived timer")
                if page<4 {board.swipeLeft()}
            }
            // The gallery's accessibility label can include leading whitespace.
            let install=board.buttons.matching(NSPredicate(format:"label CONTAINS[c] 'Add Widget'")).firstMatch
            if install.exists && install.isHittable {install.tap()}
            else {
                // If accessibility cannot resolve the control, this fallback
                // matches its visible Add Widget button in the
                // actual 440x956 system captures (all five gallery pages).
                board.coordinate(withNormalizedOffset:CGVector(dx:0.5,dy:0.915)).tap()
            }
            let done=board.buttons["Done"]
            XCTAssertTrue(done.waitForExistence(timeout:10),board.debugDescription)
            done.tap()
            let installed=board.icons.matching(NSPredicate(format:"label == 'Salati' AND value == 'Widget'")).firstMatch
            XCTAssertTrue(installed.waitForExistence(timeout:15),board.debugDescription)
            // A widget Icon can exist while its contents are still redacted.
            // Require actual localized timeline text before preserving evidence.
            let city=installed.staticTexts[arabic ? "المدينة المنورة":"Madinah"]
            XCTAssertTrue(city.waitForExistence(timeout:60),board.debugDescription)
            XCTAssertTrue(installed.staticTexts.matching(NSPredicate(format:"label BEGINSWITH %@",arabic ? "الفجر":"Fajr")).firstMatch.exists)
            XCTAssertFalse(done.exists)
            settleSystemPresentation()
            captureScreen(prefix+" installed Home Screen widget live timer")
            let hierarchy=XCTAttachment(string:board.debugDescription)
            hierarchy.name=prefix+" installed widget accessibility";hierarchy.lifetime = .keepAlways;self.add(hierarchy)
            app.terminate()
        }
    }
    func testOnboardingGPSStatusFixtures() {
        let app=XCUIApplication()
        app.launchArguments=["-uiTestingReset","-uiTestingGPSSuccess"];launch(app)
        advanceSetup(app,to:2)
        XCTAssertTrue(identified("onboardingSelectedLocation",in:app).waitForExistence(timeout:5))
        capture("Onboarding GPS success simulated fixture")
        app.terminate();app.launchArguments=["-uiTestingReset","-uiTestingGPSDenied"];launch(app)
        advanceSetup(app,to:2)
        XCTAssertTrue(identified("locationStatus",in:app).waitForExistence(timeout:5))
        XCTAssertTrue(app.buttons["chooseLocation"].isHittable)
        capture("Onboarding GPS denied simulated fixture with manual alternative")
        for (flag,title) in [("-uiTestingGPSPermission","permission request"),("-uiTestingGPSLoading","acquiring location"),("-uiTestingGPSTimeout","timeout with retry")] {
            app.terminate();app.launchArguments=["-uiTestingReset",flag];launch(app)
            advanceSetup(app,to:2)
            XCTAssertTrue(app.buttons["chooseLocation"].isHittable)
            XCTAssertFalse(app.buttons["continueOnboarding"].isEnabled)
            let statusID=flag == "-uiTestingGPSTimeout" ? "locationStatus":"locationProgress"
            XCTAssertTrue(identified(statusID,in:app).waitForExistence(timeout:5))
            capture("Onboarding GPS "+title+" simulated fixture")
        }
    }
    func testCompactWidgetLayouts() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingShowcase","-uiTestingArabicDark"];launch(app)
        chooseCity("Madinah",app:app)
        let showcase=app.buttons["presentationShowcase"];reveal(showcase,app:app);showcase.tap()
        for id in ["medium","horizontalMedium","overnightStandard","overnightHorizontal","midnightHorizontal"] {
            let open=app.buttons["open.preview."+id];reveal(open,app:app);XCTAssertTrue(open.isHittable);open.tap()
            XCTAssertTrue(identified("rendererPreviewDetail",in:app).waitForExistence(timeout:5))
            capture("Arabic dark compact-width renderer preview "+id)
            app.navigationBars.buttons.element(boundBy:0).tap()
        }
    }
    func testAccessibilityCalendarAndHome() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingLargeText"];launch(app)
        chooseCity("Madinah",app:app)
        capture("Accessibility large text prayer home")
        app.tabBars.buttons["Calendar"].tap()
        capture("Accessibility large text calendar stable six-row area")
        XCTAssertTrue(app.buttons["calendarToday"].exists)
    }
    func testAdjustedCalendarGeometryAndSwipes() {
        for arabic in [false,true] {
            let app=XCUIApplication()
            app.launchArguments=["-uiTestingReset","-uiTestingCalendarGeometry"]+(arabic ? ["-uiTestingArabicDark"]:[])
            launch(app);chooseCity("Madinah",app:app)
            app.tabBars.buttons[arabic ? "التقويم":"Calendar"].tap()
            let grid=app.otherElements["calendarGrid"]
            XCTAssertTrue(grid.waitForExistence(timeout:5))
            XCTAssertEqual(grid.value as? String,"5")
            let fiveHeight=grid.frame.height
            let prefix=arabic ? "Arabic dark calendar":"English light calendar"
            capture(prefix+" five weeks with reserved empty sixth row")
            app.buttons[arabic ? "الشهر التالي":"Next month"].tap()
            // Accessibility labels are localized by the resource catalog.
            XCTAssertEqual(grid.value as? String,"6")
            XCTAssertEqual(grid.frame.height,fiveHeight,accuracy:1,"Month navigation must retain six-row reserved height")
            capture(prefix+" six weeks same cell and grid heights")
            let heading=app.staticTexts["calendarMonth"].label
            if arabic {grid.swipeLeft()}else {grid.swipeRight()}
            XCTAssertEqual(grid.value as? String,"5")
            if arabic {grid.swipeRight()}else {grid.swipeLeft()}
            XCTAssertEqual(app.staticTexts["calendarMonth"].label,heading)
            let adjustment=app.steppers["calendarAdjustment"]
            XCTAssertTrue(adjustment.exists)
            let increase=adjustment.buttons.element(boundBy:1),decrease=adjustment.buttons.element(boundBy:0)
            increase.tap();increase.tap()
            XCTAssertEqual(app.staticTexts["calendarAdjustmentValue"].label,"+2")
            settleSystemPresentation();capture(prefix+" plus 2 adjusted first and last days")
            assertNumericCalendarDates(app)
            decrease.tap();decrease.tap();decrease.tap();decrease.tap()
            XCTAssertEqual(app.staticTexts["calendarAdjustmentValue"].label,"-2")
            settleSystemPresentation();capture(prefix+" minus 2 adjusted first and last days")
            assertNumericCalendarDates(app)
            app.buttons["calendarReset"].tap()
            XCTAssertEqual(app.staticTexts["calendarMonth"].label,heading)
            app.buttons["calendarToday"].tap()
            capture(prefix+" Today restored after adjustment reset")
            app.terminate()
        }
    }
    private func assertNumericCalendarDates(_ app:XCUIApplication) {
        let cells=app.buttons.matching(NSPredicate(format:"identifier BEGINSWITH 'calendarCell.'"))
        XCTAssertGreaterThan(cells.count,20)
        for cell in cells.allElementsBoundByIndex {
            guard let numeric=cell.value as? String else {XCTFail("Cell lacks numeric Gregorian value");continue}
            XCTAssertNotNil(numeric.range(of:"^[0-9]{1,2}/[0-9]{1,2}$",options:.regularExpression))
            let parts=cell.identifier.replacingOccurrences(of:"calendarCell.",with:"").split(separator:"-")
            XCTAssertEqual(numeric,"\(Int(parts[2])!)/\(Int(parts[1])!)")
        }
    }
    private func chooseCity(_ name:String,app:XCUIApplication,captureSetup:Bool=false) {
        let prefix=app.launchArguments.contains("-uiTestingArabicDark") ? "Arabic dark onboarding":"English light onboarding"
        XCTAssertTrue(app.buttons["continueOnboarding"].waitForExistence(timeout:10))
        if captureSetup {capture(prefix+" 1 welcome")}
        advanceSetup(app,to:2)
        XCTAssertFalse(app.buttons["continueOnboarding"].isEnabled)
        if captureSetup {capture(prefix+" 2 choose location")}
        let choose=app.buttons["chooseLocation"]
        revealOnboardingControl(choose,app:app)
        XCTAssertTrue(choose.exists && choose.isHittable,"Offline location selection must be reachable")
        XCTAssertTrue(isAboveOnboardingFooter(choose,app:app),"Location control must be fully inside the scrollable content, above the fixed footer")
        if app.launchArguments.contains("-uiTestingLargeText") {capture("Accessibility large text location action revealed above footer")}
        choose.tap()
        let search=app.searchFields.firstMatch
        XCTAssertTrue(search.waitForExistence(timeout:10));search.tap();search.typeText(name)
        let city=app.buttons.matching(NSPredicate(format:"identifier BEGINSWITH 'city.'")).firstMatch
        XCTAssertTrue(city.waitForExistence(timeout:10));city.tap()
        XCTAssertTrue(app.buttons["continueOnboarding"].isEnabled)
        if captureSetup {settleSystemPresentation();capture(prefix+" 2 offline location selected")}
        advanceSetup(app,to:3)
        if captureSetup {
            capture(prefix+" 3 ready next prayer")
            let advanced=onboardingDisclosureButton(app)
            reveal(advanced,app:app)
            XCTAssertTrue(advanced.waitForExistence(timeout:5));advanced.tap()
            capture(prefix+" 3 optional expert calculation")
            app.buttons[app.launchArguments.contains("-uiTestingArabicDark") ? "تم":"Done"].tap()
            app.buttons["onboardingBack"].tap()
            XCTAssertTrue(identified("onboardingSelectedLocation",in:app).exists)
            advanceSetup(app,to:3)
        }
        app.buttons["continueOnboarding"].tap()
        XCTAssertTrue(app.tabBars.firstMatch.waitForExistence(timeout:10))
    }
    private func isAboveOnboardingFooter(_ control:XCUIElement,app:XCUIApplication)->Bool {
        guard control.exists else {return false}
        let contentTop=app.navigationBars.firstMatch.frame.maxY
        let footerTop=app.progressIndicators.firstMatch.frame.minY-16
        return control.frame.minY>=contentTop && control.frame.maxY<=footerTop
    }
    private func assertSystemActivityDismissed(close:XCUIElement,prayer:XCUIElement,app:XCUIApplication) {
        let gone=NSPredicate {_,_ in !close.exists && !prayer.exists}
        let result=XCTWaiter.wait(for:[XCTNSPredicateExpectation(predicate:gone,object:nil)],timeout:15)
        if result != .completed {captureScreen("FAILURE Activity still visible after Close")}
        XCTAssertEqual(result,.completed,"Close must remove the actual system presentation before foreground cleanup can mask a failure")
        XCTAssertNotEqual(app.state,.runningForeground,"Close must not open Salati")
    }
    private func revealOnboardingControl(_ control:XCUIElement,app:XCUIApplication) {
        for _ in 0..<6 {
            if control.exists && control.isHittable && isAboveOnboardingFooter(control,app:app) {return}
            // Swipe within the Form's usable region. A full-screen swipe can start
            // on the fixed footer at accessibility text sizes and fail to scroll.
            let top=app.navigationBars.firstMatch.frame.maxY
            let bottom=app.progressIndicators.firstMatch.frame.minY-24
            let origin=app.coordinate(withNormalizedOffset:.zero)
            let start=origin.withOffset(CGVector(dx:app.frame.midX,dy:bottom-16))
            let end=origin.withOffset(CGVector(dx:app.frame.midX,dy:top+44))
            start.press(forDuration:0.05,thenDragTo:end)
        }
    }
    private func advanceSetup(_ app:XCUIApplication,to page:Int) {
        app.buttons["continueOnboarding"].tap()
        let state=app.staticTexts["onboardingStep"]
        let predicate=NSPredicate(format:"label == %@","\(page) / 3")
        expectation(for:predicate,evaluatedWith:state)
        waitForExpectations(timeout:5)
    }
    private func reviewAlertJourney(_ app:XCUIApplication,arabic:Bool=false) {
        // These labels match the shipped Arabic resource catalog, not translated guesses.
        let alerts=app.buttons[arabic ? "الإشعارات والأذان":"Notifications and adhan"]
        let prefix=arabic ? "Arabic dark ":"English light "
        reveal(alerts,app:app);alerts.tap()
        XCTAssertTrue(app.buttons["prayerAlert.FAJR"].waitForExistence(timeout:5))
        capture(prefix+"notification settings per-prayer overview")
        app.buttons["prayerAlert.FAJR"].tap()
        capture(prefix+"Fajr delivery and independent advance reminder")
        let delivery=app.buttons["alertDelivery"]
        XCTAssertTrue(delivery.exists);delivery.tap()
        app.buttons[arabic ? "إشعار":"Notification"].tap()
        capture(prefix+"Fajr notification sound selection")
        delivery.tap();app.buttons[arabic ? "منبه النظام":"System alarm"].tap()
        capture(prefix+"Fajr system alarm default sound and explanation")
        // Restore the no-alert preference; no system alert permission is requested here.
        delivery.tap();app.buttons[arabic ? "إيقاف":"Off"].tap()
        app.navigationBars.buttons.element(boundBy:0).tap()
        let test=app.buttons[arabic ? "اختبار تنبيه":"Test an alert"]
        reveal(test,app:app);test.tap()
        capture(prefix+"scheduled test alert distinct from immediate playback")
        XCTAssertTrue(app.buttons["scheduleTestAlert"].exists)
        app.navigationBars.buttons.element(boundBy:0).tap()
        let listen=app.buttons[arabic ? "الاستماع إلى الأذان":"Listen to adhan"]
        reveal(listen,app:app);listen.tap()
        capture(prefix+"immediate adhan recording preview list")
    }
    private func settleSystemPresentation() {
        // XCTest waits for UI idleness around its interactions; allow the final
        // system presentation animation to settle before requesting screen pixels.
        RunLoop.current.run(until:Date().addingTimeInterval(1))
    }
    private func identified(_ identifier:String,in app:XCUIApplication)->XCUIElement {
        app.descendants(matching:.any).matching(identifier:identifier).firstMatch
    }
    private func onboardingDisclosureButton(_ app:XCUIApplication)->XCUIElement {
        app.buttons["onboardingAdvanced"]
    }
    private func systemLanguage(_ arabic:Bool)->[String] {
        ["-AppleLanguages",arabic ? "(ar)":"(en)","-AppleLocale",arabic ? "ar_SA":"en_US"]
    }
    private func launch(_ app:XCUIApplication) {
        if !app.launchArguments.contains("-AppleLanguages") {
            app.launchArguments += systemLanguage(app.launchArguments.contains("-uiTestingArabicDark"))
        }
        app.launch()
    }
    private func languageControl(_ app:XCUIApplication)->XCUIElement {
        let control=app.buttons["appLanguage"]
        reveal(control,app:app)
        XCTAssertTrue(control.waitForExistence(timeout:5));return control
    }
    private enum ScrollDirection {case up,down}
    private func reveal(_ element:XCUIElement,app:XCUIApplication,direction:ScrollDirection = .up) {
        for _ in 0..<6 {
            if element.exists && element.isHittable {return}
            if direction == .up {app.swipeUp()} else {app.swipeDown()}
        }
    }
    private func assertLanguageDirection(_ app:XCUIApplication,arabic:Bool) {
        let prayer=app.tabBars.buttons[arabic ? "الصلاة":"Prayer"]
        let settings=app.tabBars.buttons[arabic ? "الإعدادات":"Settings"]
        XCTAssertTrue(prayer.waitForExistence(timeout:5));XCTAssertTrue(settings.exists)
        if arabic {XCTAssertGreaterThan(prayer.frame.midX,settings.frame.midX)}
        else {XCTAssertLessThan(prayer.frame.midX,settings.frame.midX)}
    }
    private func captureScreen(_ name:String) {
        let attachment=XCTAttachment(screenshot:XCUIScreen.main.screenshot());attachment.name=name;attachment.lifetime = .keepAlways;add(attachment)
    }
    private func capture(_ name:String) {
        let attachment=XCTAttachment(screenshot:XCUIApplication().screenshot());attachment.name=name;attachment.lifetime = .keepAlways;add(attachment)
    }
}
