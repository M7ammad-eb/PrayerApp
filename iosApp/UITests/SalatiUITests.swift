import XCTest
final class SalatiUITests:XCTestCase {
    func testChooseOfflineCityAndNavigate() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingHeading90"];app.launch()
        chooseCity("Damascus",app:app)
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
    }
    func testArabicDarkOfflineFlow() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingArabicDark"];app.launch()
        chooseCity("Madinah",app:app)
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
            for id in ["small","medium","large","inline","circular","rectangular","activity","activityExpired","compact","minimal","expanded"] {
                let preview=app.otherElements["preview."+id]
                reveal(preview,app:app)
                XCTAssertTrue(preview.exists,"Missing preview \(id)")
                capture(prefix+" "+id)
            }
            app.terminate()
        }
    }
    func testOnboardingGPSStatusFixtures() {
        let app=XCUIApplication()
        app.launchArguments=["-uiTestingReset","-uiTestingGPSSuccess"];app.launch()
        XCTAssertTrue(app.staticTexts["onboardingSelectedLocation"].waitForExistence(timeout:5))
        capture("Onboarding GPS success simulated fixture")
        app.terminate();app.launchArguments=["-uiTestingReset","-uiTestingGPSDenied"];app.launch()
        XCTAssertTrue(app.staticTexts["locationStatus"].waitForExistence(timeout:5))
        XCTAssertTrue(app.buttons["chooseLocation"].isHittable)
        capture("Onboarding GPS denied simulated fixture with manual alternative")
    }
    func testAccessibilityCalendarAndHome() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingLargeText"];app.launch()
        chooseCity("Madinah",app:app)
        capture("Accessibility large text prayer home")
        app.tabBars.buttons["Calendar"].tap()
        capture("Accessibility large text calendar list")
        XCTAssertTrue(app.buttons["calendarToday"].exists)
    }
    private func chooseCity(_ name:String,app:XCUIApplication) {
        app.buttons["chooseLocation"].tap()
        let search=app.searchFields.firstMatch
        XCTAssertTrue(search.waitForExistence(timeout:10));search.tap();search.typeText(name)
        let city=app.buttons.matching(NSPredicate(format:"identifier BEGINSWITH 'city.'")).firstMatch
        XCTAssertTrue(city.waitForExistence(timeout:10));city.tap()
        let proceed=app.buttons["continueOnboarding"]
        reveal(proceed,app:app)
        XCTAssertTrue(proceed.waitForExistence(timeout:5));XCTAssertTrue(proceed.isEnabled);proceed.tap()
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
    private func capture(_ name:String) {
        let attachment=XCTAttachment(screenshot:XCUIApplication().screenshot());attachment.name=name;attachment.lifetime = .keepAlways;add(attachment)
    }
}
