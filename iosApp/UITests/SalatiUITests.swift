import XCTest
final class SalatiUITests:XCTestCase {
    func testChooseOfflineCityAndNavigate() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingHeading90"];app.launch()
        chooseCity("Damascus",app:app,captureSetup:true)
        XCTAssertTrue(app.tabBars.buttons["Calendar"].waitForExistence(timeout:10))
        capture("Prayer home")
        app.tabBars.buttons["Calendar"].tap();capture("Hijri calendar with dual dates")
        app.buttons["Next month"].tap();app.buttons["calendarToday"].tap()
        XCTAssertTrue(app.buttons["calendarToday"].exists)
        app.tabBars.buttons["Qibla"].tap();capture("Qibla simulated true heading 90 English")
        XCTAssertTrue(app.otherElements["qiblaCompass"].exists)
        app.tabBars.buttons["Settings"].tap()
        let language=languageControl(app)
        capture("Settings Display language")
        language.buttons["العربية"].tap()
        XCTAssertTrue(app.tabBars.buttons["الإعدادات"].waitForExistence(timeout:5))
        assertLanguageDirection(app,arabic:true)
        capture("Settings immediately after switching to Arabic")
        app.tabBars.buttons["التقويم"].tap();capture("Calendar after switching to Arabic")
        app.tabBars.buttons["القبلة"].tap();capture("Qibla simulated true heading 90 Arabic")
        app.tabBars.buttons["الإعدادات"].tap()
        languageControl(app).buttons["English"].tap()
        XCTAssertTrue(app.tabBars.buttons["Settings"].waitForExistence(timeout:5))
        assertLanguageDirection(app,arabic:false)
        capture("Settings immediately after returning to English")
        // Repeat both directions and reopen a native sheet to catch host-direction regressions.
        languageControl(app).buttons["العربية"].tap()
        assertLanguageDirection(app,arabic:true)
        languageControl(app).buttons["English"].tap()
        assertLanguageDirection(app,arabic:false)
        app.swipeDown();app.swipeDown()
        let city=app.buttons.matching(NSPredicate(format:"label CONTAINS 'Damascus'")).firstMatch
        reveal(city,app:app,direction:.down)
        XCTAssertTrue(city.exists);city.tap()
        XCTAssertTrue(app.searchFields.firstMatch.waitForExistence(timeout:5))
        capture("Location sheet after repeated language transitions")
        app.buttons["Done"].tap()
        languageControl(app).buttons["العربية"].tap()
        assertLanguageDirection(app,arabic:true)
        app.terminate();app.launchArguments=[];app.launch()
        XCTAssertTrue(app.tabBars.buttons["الإعدادات"].waitForExistence(timeout:10))
        app.tabBars.buttons["الإعدادات"].tap()
        assertLanguageDirection(app,arabic:true)
        languageControl(app).buttons["English"].tap()
        assertLanguageDirection(app,arabic:false)
        capture("English settings after language persistence relaunch")
        reviewAlertJourney(app)
    }
    func testArabicDarkOfflineFlow() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingArabicDark"];app.launch()
        chooseCity("Madinah",app:app,captureSetup:true)
        XCTAssertTrue(app.tabBars.buttons["التقويم"].waitForExistence(timeout:10))
        capture("Arabic dark prayer home")
        let text=app.staticTexts.allElementsBoundByIndex.map(\.label).joined(separator:" ")
        XCTAssertNil(text.range(of:"[٠-٩۰-۹]",options:.regularExpression),"Western digits must remain independent of Arabic UI")
        app.tabBars.buttons["التقويم"].tap();capture("Arabic dark Hijri calendar")
        app.tabBars.buttons["القبلة"].tap();capture("Arabic dark Qibla")
        app.tabBars.buttons["الإعدادات"].tap();_=languageControl(app);capture("Arabic dark Settings Display")
    }
    func testWidgetAndActivityRendererPreviews() {
        for arabic in [false,true] {
            let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingShowcase"]+(arabic ? ["-uiTestingArabicDark"] : []);app.launch()
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
                captureScreen(prefix+" actual system compact Live Activity")
                // This operates the system island; it does not impose any app layout geometry.
                let springboard=XCUIApplication(bundleIdentifier:"com.apple.springboard")
                springboard.coordinate(withNormalizedOffset:CGVector(dx:0.5,dy:0.035)).press(forDuration:1)
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
    func testOnboardingGPSStatusFixtures() {
        let app=XCUIApplication()
        app.launchArguments=["-uiTestingReset","-uiTestingGPSSuccess"];app.launch()
        advanceSetup(app,to:2)
        XCTAssertTrue(identified("onboardingSelectedLocation",in:app).waitForExistence(timeout:5))
        capture("Onboarding GPS success simulated fixture")
        app.terminate();app.launchArguments=["-uiTestingReset","-uiTestingGPSDenied"];app.launch()
        advanceSetup(app,to:2)
        XCTAssertTrue(identified("locationStatus",in:app).waitForExistence(timeout:5))
        XCTAssertTrue(app.buttons["chooseLocation"].isHittable)
        capture("Onboarding GPS denied simulated fixture with manual alternative")
        for (flag,title) in [("-uiTestingGPSPermission","permission request"),("-uiTestingGPSLoading","acquiring location"),("-uiTestingGPSTimeout","timeout with retry")] {
            app.terminate();app.launchArguments=["-uiTestingReset",flag];app.launch()
            advanceSetup(app,to:2)
            XCTAssertTrue(app.buttons["chooseLocation"].isHittable)
            XCTAssertFalse(app.buttons["continueOnboarding"].isEnabled)
            let statusID=flag == "-uiTestingGPSTimeout" ? "locationStatus":"locationProgress"
            XCTAssertTrue(identified(statusID,in:app).waitForExistence(timeout:5))
            capture("Onboarding GPS "+title+" simulated fixture")
        }
    }
    func testCompactWidgetLayouts() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingShowcase","-uiTestingArabicDark"];app.launch()
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
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingLargeText"];app.launch()
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
            app.launch();chooseCity("Madinah",app:app)
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
            capture(prefix+" plus 2 adjusted first and last days")
            assertNumericCalendarDates(app)
            decrease.tap();decrease.tap();decrease.tap();decrease.tap()
            XCTAssertEqual(app.staticTexts["calendarAdjustmentValue"].label,"-2")
            capture(prefix+" minus 2 adjusted first and last days")
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
        if captureSetup {capture(prefix+" 1 language")}
        advanceSetup(app,to:2)
        XCTAssertFalse(app.buttons["continueOnboarding"].isEnabled)
        if captureSetup {capture(prefix+" 2 choose location")}
        app.buttons["chooseLocation"].tap()
        let search=app.searchFields.firstMatch
        XCTAssertTrue(search.waitForExistence(timeout:10));search.tap();search.typeText(name)
        let city=app.buttons.matching(NSPredicate(format:"identifier BEGINSWITH 'city.'")).firstMatch
        XCTAssertTrue(city.waitForExistence(timeout:10));city.tap()
        XCTAssertTrue(app.buttons["continueOnboarding"].isEnabled)
        if captureSetup {capture(prefix+" 2 offline location selected")}
        advanceSetup(app,to:3)
        if captureSetup {
            capture(prefix+" 3 automatic calculation")
            let advanced=onboardingDisclosureButton(app)
            reveal(advanced,app:app)
            XCTAssertTrue(advanced.waitForExistence(timeout:5));advanced.tap()
            capture(prefix+" 3 optional expert calculation")
            onboardingDisclosureButton(app).tap()
            app.buttons["onboardingBack"].tap()
            XCTAssertTrue(identified("onboardingSelectedLocation",in:app).exists)
            advanceSetup(app,to:3)
        }
        advanceSetup(app,to:4)
        if captureSetup {capture(prefix+" 4 optional alerts skipped")}
        advanceSetup(app,to:5)
        if captureSetup {capture(prefix+" 5 saved setup summary")}
        app.buttons["continueOnboarding"].tap()
        XCTAssertTrue(app.tabBars.firstMatch.waitForExistence(timeout:10))
    }
    private func advanceSetup(_ app:XCUIApplication,to page:Int) {
        app.buttons["continueOnboarding"].tap()
        let state=app.staticTexts["onboardingStep"]
        let predicate=NSPredicate(format:"label == %@","\(page) / 5")
        expectation(for:predicate,evaluatedWith:state)
        waitForExpectations(timeout:5)
    }
    private func reviewAlertJourney(_ app:XCUIApplication) {
        let alerts=app.buttons["Notifications and adhan"]
        reveal(alerts,app:app);alerts.tap()
        XCTAssertTrue(app.buttons["prayerAlert.FAJR"].waitForExistence(timeout:5))
        capture("Notification settings per-prayer overview")
        app.buttons["prayerAlert.FAJR"].tap()
        capture("Fajr delivery and independent advance reminder")
        let delivery=app.buttons["alertDelivery"]
        XCTAssertTrue(delivery.exists);delivery.tap()
        app.buttons["Notification"].tap()
        capture("Fajr notification sound selection")
        delivery.tap();app.buttons["System alarm"].tap()
        capture("Fajr system alarm full recording selection")
        // Restore the no-alert preference; no system alert permission is requested here.
        delivery.tap();app.buttons["Off"].tap()
        app.navigationBars.buttons.element(boundBy:0).tap()
        reveal(app.buttons["Test an alert"],app:app);app.buttons["Test an alert"].tap()
        capture("Scheduled test alert distinct from immediate playback")
        XCTAssertTrue(app.buttons["scheduleTestAlert"].exists)
        app.navigationBars.buttons.element(boundBy:0).tap()
        reveal(app.buttons["Listen to adhan"],app:app);app.buttons["Listen to adhan"].tap()
        capture("Immediate adhan recording preview list")
    }
    private func identified(_ identifier:String,in app:XCUIApplication)->XCUIElement {
        app.descendants(matching:.any).matching(identifier:identifier).firstMatch
    }
    private func onboardingDisclosureButton(_ app:XCUIApplication)->XCUIElement {
        // SwiftUI may expose a DisclosureGroup identifier on its container.
        // Select the disclosure header's actual button, never tap the group's centre.
        let labels=NSPredicate(format:"label BEGINSWITH %@ OR label BEGINSWITH %@","Advanced settings","إعدادات متقدمة")
        let container=identified("onboardingAdvanced",in:app)
        let header=container.buttons.matching(labels).firstMatch
        if header.exists {return header}
        let globalHeader=app.buttons.matching(labels).firstMatch
        if globalHeader.exists {return globalHeader}
        // The collapsed control can itself be the button, with a system-generated label.
        return app.buttons.matching(identifier:"onboardingAdvanced").firstMatch
    }
    private func languageControl(_ app:XCUIApplication)->XCUIElement {
        let control=app.segmentedControls["appLanguage"]
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
        let control=languageControl(app)
        let english=control.buttons["English"],arabicButton=control.buttons["العربية"]
        let direction=NSPredicate {_,_ in
            guard english.exists && arabicButton.exists else {return false}
            return arabic ? english.frame.minX>arabicButton.frame.minX : english.frame.minX<arabicButton.frame.minX
        }
        expectation(for:direction,evaluatedWith:nil)
        waitForExpectations(timeout:5)
    }
    private func captureScreen(_ name:String) {
        let attachment=XCTAttachment(screenshot:XCUIScreen.main.screenshot());attachment.name=name;attachment.lifetime = .keepAlways;add(attachment)
    }
    private func capture(_ name:String) {
        let attachment=XCTAttachment(screenshot:XCUIApplication().screenshot());attachment.name=name;attachment.lifetime = .keepAlways;add(attachment)
    }
}
